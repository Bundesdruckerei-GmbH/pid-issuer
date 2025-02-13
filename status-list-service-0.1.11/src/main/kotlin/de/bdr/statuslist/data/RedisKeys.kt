/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.data

/** The redis keys used to store information. */
object RedisKeys {

    fun poolConfig(poolId: String) = "pool:config:$poolId"

    fun poolCurrentLists(poolId: String) = "pool:lists:current:$poolId"

    fun poolPrecreationFlag(poolId: String) = "pool:precreation:$poolId"

    fun poolAllLists(poolId: String) = "pool:lists:all:$poolId"

    fun listData(uri: String) = "list:data:$uri"

    fun listConfig(uri: String) = "list:config:$uri"

    fun listIndices(uri: String) = "list:indices:$uri"

    fun aofTrigger() = "aofTrigger"
}
