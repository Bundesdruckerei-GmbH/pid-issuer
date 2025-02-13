/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.data

import de.bdr.statuslist.config.AppConfiguration
import de.bdr.statuslist.config.TransactionalOutsideBean
import de.bdr.statuslist.data.Storage.ListConfig
import de.bdr.statuslist.stats.StatsService
import de.bdr.statuslist.util.log
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.postgresql.util.PSQLException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.NoTransactionException
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.support.TransactionSynchronizationManager

private object Const {
    const val UUID_LENGTH = 36
    const val POSTGRES_INDEX_OFFSET = 1
}

@Component
@ConditionalOnProperty("app.storage-type", havingValue = "postgres")
class PostgreSqlStorage(
    private val config: AppConfiguration,
    private val poolAdapter: PoolAdapter,
    private val listAdapter: ListAdapter,
    private val listIndexAdapter: ListIndexAdapter,
    private val statsService: StatsService,
) : Storage {
    private val listConfigCache = ConcurrentHashMap<String, ListConfig>()

    private val retryTemplate =
        RetryTemplate.builder()
            .maxAttempts(10)
            .fixedBackoff(500)
            .retryOn(PSQLException::class.java)
            .traversingCauses()
            .build()

    override fun newListOf(listUri: String): Storage.List {
        return List(listUri)
    }

    override fun newPoolOf(poolId: String): Storage.Pool {
        return Pool(poolId)
    }

    open inner class List(private val listUri: String) : Storage.List() {
        private val id: UUID
        private val baseUri: String

        init {
            val splitIndex = listUri.length - Const.UUID_LENGTH
            check(splitIndex > 0) { "listUri $listUri invalid" }
            id = UUID.fromString(listUri.substring(splitIndex, listUri.length))
            baseUri = listUri.substring(0, splitIndex)
        }

        @TransactionalOutsideBean
        override fun create(
            generatedIndices: kotlin.collections.List<Int>,
            list: ListConfig,
            immediatelyTaken: kotlin.collections.List<Int>,
        ) {
            val poolId =
                poolAdapter.findPoolIdByName(list.poolId)
                    ?: error("pool ${list.poolId} not present")
            val data = "0".repeat(list.size * list.bits)
            listAdapter.createList(id, poolId, baseUri, list.bits, list.size, list.version, data)

            if (generatedIndices.isNotEmpty()) {
                listIndexAdapter.saveIndices(generatedIndices, id)
            }

            statsService.listCreated(list.poolId, listUri, list.size)
            if (immediatelyTaken.isNotEmpty()) {
                statsService.indicesTaken(list.poolId, listUri, immediatelyTaken.size)
            }
        }

        override fun updateStatus(list: ListConfig, value: Int, offsetInList: Int) {
            retryTemplate.execute<Unit, Throwable> {
                updateStatusRetryable(list, value, offsetInList)
            }
        }

        @TransactionalOutsideBean(isolation = Isolation.REPEATABLE_READ)
        private fun updateStatusRetryable(list: ListConfig, value: Int, offsetInList: Int) {
            val pgOffset = offsetInList + Const.POSTGRES_INDEX_OFFSET
            listAdapter.updateDataAndVersion(id, value, pgOffset, list.bits, 1)
            statsService.statusUpdated(list.poolId, listUri)
        }

        override fun config(allowCache: Boolean): ListConfig {
            return config(allowCache, listUri, listConfigCache)
        }

        @TransactionalOutsideBean(readOnly = true)
        override fun readConfig(): ListConfig {
            val current =
                listAdapter.findListAttributesById(id) ?: error("Missing list config $listUri")
            return ListConfig(current.bits, current.size, current.poolName, id, current.version)
        }

        @TransactionalOutsideBean(readOnly = true)
        override fun data(): ByteArray {
            val data = listAdapter.findDataById(id) ?: error("list $id not found")
            return encodeToByteArray(data)
        }

        @TransactionalOutsideBean(readOnly = true)
        override fun poolId(): String {
            return config().poolId
        }

        @TransactionalOutsideBean
        override fun freeIndices(indices: kotlin.collections.List<Int>) {
            listIndexAdapter.saveIndices(indices, id)
            statsService.indicesFreed(config().poolId, listUri, indices.size)
        }

        override fun take(maxAmount: Int): kotlin.collections.List<Int> {
            return retryTemplate.execute<kotlin.collections.List<Int>, Throwable> {
                takeRetryable(maxAmount)
            }
        }

        @TransactionalOutsideBean(isolation = Isolation.REPEATABLE_READ)
        private fun takeRetryable(maxAmount: Int): kotlin.collections.List<Int> {
            val taken = listIndexAdapter.takeIndices(id, maxAmount)
            statsService.indicesTaken(config().poolId, listUri, taken.size)
            return taken
        }

        @TransactionalOutsideBean(readOnly = true)
        override fun isFull(): Boolean {
            val fullSize = listAdapter.findSizeById(id)
            val actualSize = listIndexAdapter.countIndices(id)
            return fullSize == actualSize
        }

        @TransactionalOutsideBean(readOnly = true)
        override fun isEmpty(): Boolean {
            return listIndexAdapter.countIndices(id) == 0
        }
    }

    open inner class Pool(poolName: String) : Storage.Pool(poolName, config.publicUrl) {
        @TransactionalOutsideBean
        override fun createOrVerifyPool(pool: Storage.PoolConfig): Storage.PoolConfig {
            var storedPoolConfig = config()

            if (!poolAdapter.existsByName(poolName)) {
                poolAdapter.createPool(pool.bits, pool.size, poolName)
                storedPoolConfig = pool.copy(creation = Storage.CreationStatus.DONE)
                log.info("Pool $poolName created")
            }

            return storedPoolConfig ?: error("Pool config does not exist")
        }

        @TransactionalOutsideBean(readOnly = true)
        override fun config(): Storage.PoolConfig? {
            return poolAdapter.findPoolAttributesByName(poolName)?.let {
                Storage.PoolConfig(it.bits, it.size, Storage.CreationStatus.DONE)
            }
        }

        override fun listOf(listUri: String): Storage.List {
            return newListOf(listUri)
        }

        @TransactionalOutsideBean(readOnly = true)
        override fun currentLists(): kotlin.collections.List<String> {
            val poolId =
                poolAdapter.findPoolIdByName(poolName) ?: error("pool $poolName does not exist")
            return listAdapter.findAllListUrisWithIndices(poolId)
        }

        @TransactionalOutsideBean(readOnly = true)
        override fun allListUris(): Iterable<String> {
            val poolId =
                poolAdapter.findPoolIdByName(poolName) ?: error("pool $poolName does not exist")
            return listAdapter.findAllListUris(poolId)
        }

        override fun obtainPrecreationLock(): Closeable? {
            if (!TransactionSynchronizationManager.isActualTransactionActive()) {
                throw NoTransactionException(
                    "Required to run in a transaction to acquire a transaction lifecycle bound lock"
                )
            }
            val locked = poolAdapter.obtainAdvisoryLock(poolName)

            log.debug("Lock obtained for {}: {}", poolName, locked)
            return if (locked) Closeable { /* nothing */ } else null
        }
    }

    private fun encodeToByteArray(bitString: String): ByteArray {
        check(bitString.length % 8 == 0) { "Invalid size of bit string" }
        val bits = bitString.map { it.digitToInt() }
        val results = ByteArray(bits.size / 8)
        var byteValue = 0
        for (i in bits.indices) {
            byteValue = (byteValue shl 1) or bits[i]

            if (i % 8 == 7) {
                results[i / 8] = byteValue.toByte()
            }
        }
        return results
    }
}
