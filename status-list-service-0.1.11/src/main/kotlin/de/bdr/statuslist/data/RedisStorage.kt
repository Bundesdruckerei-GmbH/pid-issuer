/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.data

import de.bdr.statuslist.config.AppConfiguration
import de.bdr.statuslist.config.PersistenceStrategy.APPEND_FSYNC_ALWAYS
import de.bdr.statuslist.data.Storage.ListConfig
import de.bdr.statuslist.stats.StatsService
import de.bdr.statuslist.util.log
import jakarta.annotation.PostConstruct
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.connection.BitFieldSubCommands
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("app.storage-type", havingValue = "redis")
class RedisStorage(
    private val config: AppConfiguration,
    private val redis: RedisTemplate<String, String>,
    private val byteRedis: RedisTemplate<String, ByteArray>,
    private val statsService: StatsService,
) : Storage {

    private val listConfigCache = ConcurrentHashMap<String, ListConfig>()

    @PostConstruct
    fun validateRedisConfig() {
        if (config.redis.persistenceStrategy == APPEND_FSYNC_ALWAYS) {
            val result = getAppendFsync()
            check(result == "always") { "appendfsync not set to always in redis" }
        }
    }

    override fun newListOf(listUri: String): Storage.List {
        return this.List(listUri)
    }

    override fun newPoolOf(poolId: String): Storage.Pool {
        return this.Pool(poolId)
    }

    inner class List(private val listUri: String) : Storage.List() {

        override fun create(
            generatedIndices: kotlin.collections.List<Int>,
            list: ListConfig,
            immediatelyTaken: kotlin.collections.List<Int>,
        ) {
            if (generatedIndices.isNotEmpty()) {
                redis
                    .opsForList()
                    .rightPushAll(
                        RedisKeys.listIndices(listUri),
                        generatedIndices.map { it.toString() },
                    )
            }

            redis
                .opsForValue()
                .bitField(
                    RedisKeys.listData(listUri),
                    BitFieldSubCommands.create()
                        .set(BitFieldSubCommands.BitFieldType.unsigned(1))
                        .valueAt((list.size * list.bits - 1).toLong())
                        .to(0),
                )
            redis
                .opsForHash<String, String>()
                .putAll(
                    RedisKeys.listConfig(listUri),
                    mapOf(
                        "bits" to list.bits.toString(),
                        "size" to list.size.toString(),
                        "poolId" to list.poolId,
                        "listId" to list.listId.toString(),
                        "version" to list.version.toString(),
                    ),
                )
            redis.opsForSet().add(RedisKeys.poolAllLists(list.poolId), listUri)

            if (generatedIndices.isNotEmpty()) {
                redis.opsForList().rightPush(RedisKeys.poolCurrentLists(list.poolId), listUri)
            }

            statsService.listCreated(list.poolId, listUri, list.size)
            if (immediatelyTaken.isNotEmpty()) {
                statsService.indicesTaken(list.poolId, listUri, immediatelyTaken.size)
            }
        }

        override fun updateStatus(list: ListConfig, value: Int, offsetInList: Int) {
            redis
                .opsForValue()
                .bitField(
                    RedisKeys.listData(listUri),
                    BitFieldSubCommands.create()
                        .set(BitFieldSubCommands.BitFieldType.signed(list.bits))
                        .valueAt(offsetInList.toLong())
                        .to(value.toLong()),
                )
            redis
                .opsForHash<String, String>()
                .increment(RedisKeys.listConfig(listUri), "version", 1)
            statsService.statusUpdated(list.poolId, listUri)
        }

        override fun config(allowCache: Boolean): ListConfig {
            return config(allowCache, listUri, listConfigCache)
        }

        override fun readConfig(): ListConfig {
            val config = redis.opsForHash<String, String>().entries(RedisKeys.listConfig(listUri))
            check(config.isNotEmpty()) { "Missing list config $listUri" }
            return ListConfig(
                config["bits"]?.toIntOrNull() ?: error("Invalid bits for list $listUri"),
                config["size"]?.toIntOrNull() ?: error("Invalid size for list $listUri"),
                config["poolId"] ?: error("No poolId for list $listUri"),
                UUID.fromString(config["listId"] ?: error("No listId for list $listUri")),
                config["version"]?.toIntOrNull() ?: error("Invalid version for list $listUri"),
            )
        }

        override fun data() =
            byteRedis.opsForValue()[RedisKeys.listData(listUri)]
                ?: error("No list data in redis for list $listUri")

        override fun poolId() = config().poolId

        override fun freeIndices(indices: kotlin.collections.List<Int>) {
            redis
                .opsForList()
                .rightPushAll(RedisKeys.listIndices(listUri), indices.map { it.toString() })
            statsService.indicesFreed(config().poolId, listUri, indices.size)
        }

        override fun take(maxAmount: Int): kotlin.collections.List<Int> {
            val stringIndices =
                redis.opsForList().leftPop(RedisKeys.listIndices(listUri), maxAmount.toLong())
                    ?: emptyList()
            if (stringIndices.size < maxAmount) {
                poolId().let {
                    redis.opsForList().remove(RedisKeys.poolCurrentLists(it), 1, listUri)
                }
            }
            statsService.indicesTaken(config().poolId, listUri, stringIndices.size)
            return stringIndices.map { it.toInt() }
        }

        override fun isFull(): Boolean {
            val fullSize =
                redis.opsForHash<String, String>()[RedisKeys.listConfig(listUri), "size"]?.toLong()
            val actualSize = redis.opsForList().size(RedisKeys.listIndices(listUri))
            return fullSize == actualSize
        }

        override fun isEmpty(): Boolean {
            return redis.opsForList().size(RedisKeys.listIndices(listUri)) == 0L
        }
    }

    inner class Pool(poolName: String) : Storage.Pool(poolName, config.publicUrl) {

        override fun createOrVerifyPool(pool: Storage.PoolConfig): Storage.PoolConfig {
            var storedPoolConfig = config()
            var wait = 20L
            while (storedPoolConfig?.creation != Storage.CreationStatus.DONE) {
                check(wait <= 500L) { "Timeout waiting for pool creation" }

                if (
                    redis
                        .opsForHash<String, String>()
                        .putIfAbsent(RedisKeys.poolConfig(poolName), "creation", "pending")
                ) {
                    redis
                        .opsForHash<String, String>()
                        .putAll(
                            RedisKeys.poolConfig(poolName),
                            mapOf(
                                "bits" to "${pool.bits}",
                                "size" to "${pool.size}",
                                "creation" to Storage.CreationStatus.DONE.name,
                            ),
                        )
                    storedPoolConfig = pool.copy(creation = Storage.CreationStatus.DONE)
                    log.info("Pool $poolName created")
                } else {
                    Thread.sleep(wait)
                    wait += wait / 3
                    storedPoolConfig = config()
                }
            }
            return storedPoolConfig
        }

        override fun config(): Storage.PoolConfig? {
            val config = redis.opsForHash<String, String>().entries(RedisKeys.poolConfig(poolName))
            return if (config.isEmpty()) {
                null
            } else {
                Storage.PoolConfig(
                    config["bits"]?.toIntOrNull() ?: error("invalid bits in pool $poolName"),
                    config["size"]?.toIntOrNull() ?: error("invalid size in pool $poolName"),
                    Storage.CreationStatus.valueOf(
                        config["creation"] ?: error("invalid creation in pool $poolName")
                    ),
                )
            }
        }

        override fun listOf(listUri: String): Storage.List {
            return newListOf(listUri)
        }

        override fun currentLists(): kotlin.collections.List<String> {
            return redis.opsForList().range(RedisKeys.poolCurrentLists(poolName), 0, -1)
                ?: emptyList()
        }

        override fun allListUris(): Iterable<String> {
            return redis.opsForSet().members(RedisKeys.poolAllLists(poolName)) ?: emptyList()
        }

        override fun obtainPrecreationLock(): Closeable? {
            val poolConfig =
                config.statusListPools[poolName] ?: error("Missing config for pool $poolName")
            val locked =
                redis
                    .opsForValue()
                    .setIfAbsent(
                        RedisKeys.poolPrecreationFlag(poolName),
                        "true",
                        poolConfig.precreation.checkDelay.minusMillis(1),
                    ) ?: error("Failed to set precreation flag")

            return if (locked) {
                return Closeable { redis.delete(RedisKeys.poolPrecreationFlag(poolName)) }
            } else {
                null
            }
        }

        fun addToCurrent(listUri: String) {
            redis.opsForList().rightPush(RedisKeys.poolCurrentLists(poolName), listUri)
        }
    }

    private fun getAppendFsync(): String {
        return String(
            redis.execute { connection ->
                connection.execute("CONFIG", "GET".toByteArray(), "appendfsync".toByteArray())
            } as ByteArray
        )
    }
}
