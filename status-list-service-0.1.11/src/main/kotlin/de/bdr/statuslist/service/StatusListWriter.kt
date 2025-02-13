/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.bdr.openid4vc.statuslist.StatusList
import de.bdr.openid4vc.statuslist.StatusListToken
import de.bdr.statuslist.config.AppConfiguration
import de.bdr.statuslist.config.StatusListPoolConfiguration
import de.bdr.statuslist.data.Storage
import de.bdr.statuslist.util.measureRuntime
import java.time.Duration
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.FixedRateTask
import org.springframework.scheduling.config.ScheduledTaskRegistrar

@Configuration
@EnableScheduling
class StatusListWriter(
    val config: AppConfiguration,
    val storage: Storage,
    val aggregationIdService: AggregationIdService,
    val statusListTokenStorage: StatusListTokenStorage,
) : SchedulingConfigurer {

    private val log = LoggerFactory.getLogger(StatusListWriter::class.java)

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        config.statusListPools.forEach { (poolId, poolConfig) ->
            taskRegistrar.scheduleFixedRateTask(
                FixedRateTask({ writePoolTokens(poolId) }, poolConfig.updateInterval, Duration.ZERO)
            )
        }
    }

    fun writePoolTokens(poolId: String) {
        statusListTokenStorage.obtainPoolLock(poolId)?.use {
            log.trace("Writing JWTs for pool $poolId")
            storage.newPoolOf(poolId).allListUris().forEach { writeListToken(it) }
        }
    }

    fun writeListToken(uri: String) {
        val (duration, listConfigAndUpdated) =
            measureRuntime {
                val listConfig = storage.newListOf(uri).config(allowCache = false)
                val poolConfig =
                    config.statusListPools[listConfig.poolId]
                        ?: error("Missing pool configuration ${listConfig.poolId}")
                val metadata = statusListTokenStorage.metadata(listConfig.listId)
                if (
                    metadata.isUpdated(listConfig.version) ||
                        metadata.mayExpireAfterTwo(poolConfig.updateInterval)
                ) {
                    writeListTokens(uri, poolConfig, listConfig)
                    Pair(listConfig, true)
                } else {
                    Pair(listConfig, false)
                }
            }
        val (listConfig, updated) = listConfigAndUpdated

        if (updated) {
            log.debug(
                "Writing list {} (pool {}) took {}",
                listConfig.listId,
                listConfig.poolId,
                duration,
            )
        } else {
            log.trace("List {} (pool {}) not updated", listConfig.listId, listConfig.poolId)
        }
    }

    private fun writeListTokens(
        uri: String,
        poolConfig: StatusListPoolConfiguration,
        listConfig: Storage.ListConfig,
    ) {
        val data = storage.newListOf(uri).data()

        val expiresAt = Instant.now().plus(poolConfig.listLifetime)
        val statusListToken = createToken(uri, data, poolConfig, expiresAt, listConfig.poolId)

        val statusListJwt =
            statusListToken.asJwt(poolConfig.signerInstance) { header, _ ->
                poolConfig.modifyJwsHeader(header)
            }
        val statusListCwt =
            statusListToken.asCwt(poolConfig.signerInstance) { _, sign1 ->
                poolConfig.modifyCwtHeader(sign1)
            }
        val statusListCbor = statusListToken.statusList.toCborObject(statusListToken.aggregationUri)
        val statusListJson = statusListToken.statusList.toJsonObject(statusListToken.aggregationUri)

        statusListTokenStorage.store(
            listConfig.listId,
            TokenFormat.JWT,
            statusListJwt.serialize().toByteArray(),
        )
        statusListTokenStorage.store(
            listConfig.listId,
            TokenFormat.CWT,
            statusListCwt.EncodeToBytes(),
        )
        statusListTokenStorage.store(
            listConfig.listId,
            TokenFormat.JSON,
            ObjectMapper().writeValueAsString(statusListJson).toByteArray(),
        )
        statusListTokenStorage.store(
            listConfig.listId,
            TokenFormat.CBOR,
            statusListCbor.EncodeToBytes(),
        )

        statusListTokenStorage.storeMetadata(
            ListStorageMetadata(listConfig.listId, listConfig.version, expiresAt)
        )
    }

    private fun createToken(
        uri: String,
        data: ByteArray,
        pool: StatusListPoolConfiguration,
        expiresAt: Instant,
        poolId: String,
    ) =
        StatusListToken(
            statusListUri = uri,
            statusList = StatusList(pool.size, pool.bits, list = data),
            issuedAt = Instant.now(),
            expiresAt = expiresAt,
            issuerUri = pool.issuer,
            ttl = pool.updateInterval,
            aggregationUri =
                "${config.publicUrl}/aggregation/${aggregationIdService.aggregationIdByPoolId(poolId)}",
        )
}
