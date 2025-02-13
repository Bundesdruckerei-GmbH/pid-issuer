/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.service

import de.bdr.statuslist.config.AppConfiguration
import de.bdr.statuslist.data.RedisStorage
import de.bdr.statuslist.data.Storage
import java.util.concurrent.TimeUnit.DAYS
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NonEmptyListRevivalService(
    private val storage: Storage,
    private val config: AppConfiguration,
) {

    private val log = LoggerFactory.getLogger(NonEmptyListRevivalService::class.java)

    @Scheduled(fixedDelay = 1, timeUnit = DAYS)
    fun reviveNonEmptyLists() {
        config.statusListPools.keys.forEach(::reviveNonEmptyLists)
    }

    private fun reviveNonEmptyLists(poolId: String) {
        val pool = storage.newPoolOf(poolId)
        val all = pool.allListUris()
        val current = pool.currentLists().toSet()
        val inactive = all.toMutableSet().apply { removeAll(current) }
        inactive.forEach { listUri ->
            if (!storage.newListOf(listUri).isEmpty()) {
                if (pool is RedisStorage.Pool) {
                    pool.addToCurrent(listUri)
                }
                log.info("Revived non empty list $listUri in pool $poolId")
            }
        }
    }
}
