/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.service

import de.bdr.statuslist.config.AppConfiguration
import de.bdr.statuslist.data.Storage
import de.bdr.statuslist.stats.StatsService
import de.bdr.statuslist.util.log
import de.bdr.statuslist.util.measureRuntime
import java.util.concurrent.atomic.AtomicBoolean
import org.springframework.context.SmartLifecycle
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.stereotype.Service

@Service
class StatusListService(
    private val config: AppConfiguration,
    private val storage: Storage,
    private val statusListWriter: StatusListWriter,
    private val lettuceConnectionFactory: LettuceConnectionFactory?,
    private val aggregationIdService: AggregationIdService,
    private val statsService: StatsService,
) : SmartLifecycle {

    private var indexPrefetchers = mutableMapOf<String, IndexPrefetcher>()

    private val running = AtomicBoolean()

    override fun getPhase() = (lettuceConnectionFactory?.phase ?: 0) - 1

    override fun start() {
        config.statusListPools.forEach { (poolId, poolConfig) ->
            val pool =
                storage
                    .newPoolOf(poolId)
                    .createOrVerifyPool(
                        Storage.PoolConfig(bits = poolConfig.bits, size = poolConfig.size)
                    )
            check(pool.bits == poolConfig.bits) {
                "Stored value for bits does not match configuration for pool $poolId"
            }
            check(pool.size == poolConfig.size) {
                "Stored value for size does not match configuration for pool $poolId"
            }
            indexPrefetchers.put(
                poolId,
                IndexPrefetcher(poolId, config, statusListWriter, storage, statsService),
            )
        }
        running.set(true)
    }

    override fun isRunning() = running.get()

    override fun stop() {
        running.set(false)
        log.info("Shutting down prefetchers...")
        val duration =
            measureRuntime { indexPrefetchers.values.forEach(IndexPrefetcher::shutdown) }.first
        log.info("Prefetchers shutdown in $duration")
    }

    /**
     * Reserves `amount` status from the pool identifier by `poolId`.
     *
     * @return A list of references to the reserved status
     */
    fun reserve(poolId: String, amount: Int): List<Reference> {
        return indexPrefetchers[poolId]?.nextIndices(amount) ?: error("No such pool $poolId")
    }

    fun listsForAggregationId(aggregationId: String): List<String> {
        val poolId = aggregationIdService.poolIdByAggregationId(aggregationId)
        return storage.newPoolOf(poolId).allListUris().toList()
    }

    fun updateStatus(uri: String, index: Int, value: Int) {
        storage.newListOf(uri).updateStatus(index, value)
        log.trace("Status $uri#$index = $value")
    }

    fun poolId(listUri: String) = storage.newListOf(listUri).poolId()
}
