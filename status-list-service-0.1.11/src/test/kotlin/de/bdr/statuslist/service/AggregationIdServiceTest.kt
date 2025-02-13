/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.service

import assertk.assertThat
import assertk.assertions.isEqualTo
import de.bdr.statuslist.config.AppConfiguration
import de.bdr.statuslist.config.PrecreationConfiguration
import de.bdr.statuslist.config.PrefetchConfiguration
import de.bdr.statuslist.config.RedisConnectionConfiguration
import de.bdr.statuslist.config.SignerConfiguration
import de.bdr.statuslist.config.StatusListPoolConfiguration
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import java.time.Duration
import org.junit.jupiter.api.Test

class AggregationIdServiceTest {

    @MockK val redisConnectionConfiguration: RedisConnectionConfiguration = mockk()
    @MockK val precreationConfiguration: PrecreationConfiguration = mockk()

    val prefetchConfiguration = PrefetchConfiguration(threshold = 50, capacity = 100)

    private val appConfiguration =
        AppConfiguration(
            publicUrl = "http://localhost:8080",
            storageDirectory = "status-lists",
            cacheDuration = Duration.parse("PT24H"),
            statusListPools =
                mapOf(
                    "pool_id-1" to
                        StatusListPoolConfiguration(
                            apiKey = "apikey-1",
                            size = 128,
                            bits = 1,
                            issuer = "issuer",
                            precreation = precreationConfiguration,
                            prefetch = prefetchConfiguration,
                            updateInterval = Duration.ofMinutes(1),
                            listLifetime = Duration.ofHours(1),
                            aggregationId = "aggregationId-1",
                            signer =
                                SignerConfiguration(
                                    "classpath:/keys/pid_issuer_multi_chain.p12",
                                    "test",
                                ),
                        ),
                    "pool_id-2" to
                        StatusListPoolConfiguration(
                            apiKey = "apikey-2",
                            size = 128,
                            bits = 1,
                            issuer = "issuer",
                            precreation = precreationConfiguration,
                            prefetch = prefetchConfiguration,
                            updateInterval = Duration.ofMinutes(1),
                            listLifetime = Duration.ofHours(1),
                            aggregationId = "aggregationId-2",
                            signer =
                                SignerConfiguration(
                                    "classpath:/keys/pid_issuer_multi_chain.p12",
                                    "test",
                                ),
                        ),
                ),
            redis = redisConnectionConfiguration,
        )
    private val service = AggregationIdService(appConfiguration)

    @Test
    fun `should return pool id by given aggregation id`() {
        assertThat(service.poolIdByAggregationId("aggregationId-1")).isEqualTo("pool_id-1")
        assertThat(service.poolIdByAggregationId("aggregationId-2")).isEqualTo("pool_id-2")
    }

    @Test
    fun `should return aggregation id by given pool id`() {
        assertThat(service.aggregationIdByPoolId("pool_id-1")).isEqualTo("aggregationId-1")
        assertThat(service.aggregationIdByPoolId("pool_id-2")).isEqualTo("aggregationId-2")
    }
}
