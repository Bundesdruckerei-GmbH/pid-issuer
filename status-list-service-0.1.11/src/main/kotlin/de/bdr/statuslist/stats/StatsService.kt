/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.stats

interface StatsService {

    fun listCreated(poolId: String, listUri: String, size: Int)

    fun statusUpdated(poolId: String, listUri: String)

    fun indicesFreed(poolId: String, listUri: String, amount: Int)

    fun indicesTaken(poolId: String, listUri: String, amount: Int)

    fun indicesReserved(poolId: String, listUri: String, amount: Int)

    fun listCacheEvent(listUri: String, event: ListCacheEvent)

    enum class ListCacheEvent {
        CACHE_MISS,
        CACHE_HIT,
        CACHE_SOFT_REFERENCE_CLEARED,
    }
}
