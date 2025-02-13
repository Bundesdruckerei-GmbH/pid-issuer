/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.stats

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LoggingStatsService : StatsService {

    private val log = LoggerFactory.getLogger(LoggingStatsService::class.java)

    override fun listCreated(poolId: String, listUri: String, size: Int) {
        log.info("List created (poolId: $poolId, listUri: $listUri, size: $size)")
    }

    override fun statusUpdated(poolId: String, listUri: String) {
        log.info("Status updated (poolId: $poolId, listUri: $listUri)")
    }

    override fun indicesReserved(poolId: String, listUri: String, amount: Int) {
        log.info("Indices reserved (poolId: $poolId, listUri: $listUri, amount: $amount)")
    }

    override fun listCacheEvent(listUri: String, event: StatsService.ListCacheEvent) {
        log.info("List cache event (listUri: $listUri, event: $event)")
    }

    override fun indicesFreed(poolId: String, listUri: String, amount: Int) {
        log.info("Indices freed (poolId: $poolId, listUri: $listUri, amount: $amount)")
    }

    override fun indicesTaken(poolId: String, listUri: String, amount: Int) {
        log.info("Indices taken (poolId: $poolId, listUri: $listUri, amount: $amount")
    }
}
