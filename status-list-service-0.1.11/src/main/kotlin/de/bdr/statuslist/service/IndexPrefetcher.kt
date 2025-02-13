/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.service

import de.bdr.statuslist.config.AppConfiguration
import de.bdr.statuslist.config.OnUnderflowBehavior.DELAY
import de.bdr.statuslist.config.OnUnderflowBehavior.FAIL
import de.bdr.statuslist.config.TransactionalOutsideBean
import de.bdr.statuslist.data.Storage
import de.bdr.statuslist.stats.StatsService
import de.bdr.statuslist.util.log
import de.bdr.statuslist.util.measureRuntime
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

class IndexPrefetcher(
    private val poolId: String,
    private val appConfig: AppConfiguration,
    private val statsListWriter: StatusListWriter,
    private val storage: Storage,
    private val statsService: StatsService,
) {

    private val config =
        appConfig.statusListPools[poolId] ?: error("Missing pool $poolId in app configuration")

    // using a single thread to avoid synchronization of needed/fetched access
    private val executor = Executors.newSingleThreadScheduledExecutor()

    // used to track prefetch requirement
    // access to those variables can be without synchronization because access is serialized through
    // a single thread in executor
    private var needed: Int = 0
    private var fetched: Int = 0

    private val underflowLogExecutor = Executors.newSingleThreadScheduledExecutor()
    private val underflow = AtomicLong()

    /** Contains references that are guaranteed to be persisted in redis */
    private val availableReferences = LinkedBlockingDeque<Reference>()

    init {
        prefetch(config.prefetch.capacity)

        executor.scheduleAtFixedRate(
            { checkPrecreation() },
            0,
            config.precreation.checkDelay.toMillis(),
            MILLISECONDS,
        )

        underflowLogLoop()
    }

    private fun underflowLogLoop() {
        val amount = underflow.getAndUpdate { 0 }
        if (amount > 0) {
            log.warn("Prefetch buffer underflown by $amount for pool $poolId")
            underflowLogExecutor.schedule({ underflowLogLoop() }, 1, SECONDS)
        } else {
            underflowLogExecutor.schedule({ underflowLogLoop() }, 1, MILLISECONDS)
        }
    }

    fun nextIndices(amount: Int): List<Reference> {
        check(amount > 0) { "amount must be positive" }
        ensurePrefetch(amount)
        return take(amount)
    }

    fun shutdown() {
        availableReferences
            .groupBy { it.uri }
            .forEach { (listUri, references) ->
                storage.newListOf(listUri).freeIndices(references.map { it.index })
                log.info("pool $poolId: ${references.size} references freed")
            }
    }

    private fun take(amount: Int): List<Reference> {
        val result = ArrayList<Reference>(amount)
        var needed = amount

        // try to fetch without blocking
        while (needed > 0) {
            result.add(availableReferences.poll() ?: break)
            needed--
        }

        if (needed == 0) {
            reportReserved(result)
            return result
        }

        // prefetch buffer underflown

        underflow.updateAndGet { l -> l + needed }

        when (config.prefetch.onUnderflow) {
            FAIL -> {
                result.forEach { availableReferences.add(it) }
                throw PrefetchBufferUnderflowException()
            }

            DELAY -> {
                // fetch remaining references with blocking
                val underflow = needed
                val duration =
                    measureRuntime {
                            while (needed > 0) {
                                result.add(availableReferences.take())
                                needed--
                            }
                        }
                        .first

                log.info(
                    "Pool $poolId: Waiting for additional $underflow references took $duration."
                )

                reportReserved(result)
                return result
            }
        }
    }

    private fun reportReserved(references: List<Reference>) {
        references
            .groupBy { it.uri }
            .forEach { (listUri, references) ->
                statsService.indicesReserved(
                    storage.newListOf(listUri).poolId(),
                    listUri,
                    references.size,
                )
            }
    }

    private fun ensurePrefetch(amount: Int) {
        executor.execute {
            needed += amount
            log.trace("pool {}: fetched {}, needed {}", poolId, fetched, needed)
            if (fetched - needed < config.prefetch.threshold) {
                prefetch(config.prefetch.capacity - fetched + needed)
            }
        }
    }

    private fun prefetch(needed: Int) {
        log.debug("Prefetching $needed")
        var remainingNeeded = needed
        while (remainingNeeded > 0) {
            val newFetched = prefetchFromCurrentList(remainingNeeded)
            fetched += newFetched
            remainingNeeded -= newFetched
        }
    }

    private fun prefetchFromCurrentList(maximumAmount: Int): Int {
        val usableLists = storage.newPoolOf(poolId).currentLists()
        return if (usableLists.isEmpty()) {
            val (uri, taken) = storage.newPoolOf(poolId).createList(maximumAmount)
            statsListWriter.writeListToken(uri)
            addTakenReferences(uri, taken)
            maximumAmount
        } else {
            val listUri = usableLists.first()
            val taken = storage.newListOf(listUri).take(maximumAmount)
            if (taken.isNotEmpty()) {
                addTakenReferences(listUri, taken)
            }
            taken.size
        }
    }

    private fun addTakenReferences(uri: String, taken: List<Int>) {
        availableReferences.addAll(taken.map { Reference(uri, it) })
        log.info("Made {} indices available for pool {}", taken.size, poolId)
    }

    @TransactionalOutsideBean
    private fun checkPrecreation() {
        storage.newPoolOf(poolId).obtainPrecreationLock()?.use {
            val usableLists = storage.newPoolOf(poolId).currentLists()
            val fullLists = usableLists.count { storage.newListOf(it).isFull() }
            val toCreate = max(0, config.precreation.lists - fullLists)
            if (toCreate > 0) {
                log.info("Precreating $toCreate list(s) for pool $poolId")
            }
            repeat(toCreate) {
                val (uri, _) = storage.newPoolOf(poolId).createList()
                statsListWriter.writeListToken(uri)
            }
        }
    }
}
