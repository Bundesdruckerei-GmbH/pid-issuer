/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.service

import de.bdr.statuslist.config.AppConfiguration
import org.springframework.stereotype.Service

@Service
class AggregationIdService(configuration: AppConfiguration) {

    private val poolIdByAggregationId =
        configuration.statusListPools.entries.associate {
            Pair(it.value.aggregationId ?: it.key, it.key)
        }

    private val aggregationIdByPoolId =
        poolIdByAggregationId.entries.associate { Pair(it.value, it.key) }

    fun poolIdByAggregationId(aggregationId: String): String {
        return poolIdByAggregationId[aggregationId] ?: error("Missing entry")
    }

    fun aggregationIdByPoolId(poolId: String): String {
        return aggregationIdByPoolId[poolId] ?: error("Missing entry")
    }
}
