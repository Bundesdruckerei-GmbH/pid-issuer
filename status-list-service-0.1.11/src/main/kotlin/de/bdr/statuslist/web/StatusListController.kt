/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.web

import de.bdr.statuslist.config.AppConfiguration
import de.bdr.statuslist.service.StatusListService
import de.bdr.statuslist.service.StatusListTokenSource
import de.bdr.statuslist.service.TokenFormat
import de.bdr.statuslist.stats.StatsService
import de.bdr.statuslist.stats.StatsService.ListCacheEvent.CACHE_HIT
import de.bdr.statuslist.stats.StatsService.ListCacheEvent.CACHE_MISS
import de.bdr.statuslist.stats.StatsService.ListCacheEvent.CACHE_SOFT_REFERENCE_CLEARED
import de.bdr.statuslist.web.api.DefaultApi
import de.bdr.statuslist.web.api.model.ErrorResponse
import de.bdr.statuslist.web.api.model.StatusLists
import java.lang.ref.SoftReference
import java.net.URI
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.atomic.AtomicReference
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
@ConditionalOnProperty("app.serve-status-lists", havingValue = "true", matchIfMissing = true)
class StatusListController(
    val config: AppConfiguration,
    val statusListService: StatusListService,
    val statusListTokenSource: StatusListTokenSource,
    val statsService: StatsService,
) : DefaultApi {

    companion object {
        private const val IF_MODIFIED_SINCE_NOT_SET = -1L
    }

    private val cache: ConcurrentMap<CacheKey, SoftReference<CacheEntry>> = ConcurrentHashMap()

    @Scheduled(fixedRate = 1, timeUnit = HOURS)
    fun clearUnused() {
        cache.keys.forEach { id ->
            cache.computeIfPresent(id) { _, currentValue ->
                val entry = currentValue.get()
                if (entry == null || !entry.hasBeenUsedRecently()) {
                    null
                } else {
                    currentValue
                }
            }
        }
    }

    override fun serveAggregation(
        @PathVariable(value = "pool-id") poolId: String,
        @RequestHeader headers: HttpHeaders,
    ): ResponseEntity<StatusLists> {
        val lists: List<String> =
            try {
                statusListService.listsForAggregationId(poolId)
            } catch (e: IllegalStateException) {
                throw ErrorResponseException(
                    NOT_FOUND,
                    ErrorResponse(
                        ErrorCode.NO_SUCH_POOL.name,
                        e.message ?: "No pool with id $poolId",
                    ),
                )
            }
        if (lists.isEmpty()) {
            throw ErrorResponseException(
                NOT_FOUND,
                ErrorResponse(ErrorCode.NO_SUCH_POOL.name, "No pool with id $poolId"),
            )
        }
        return ResponseEntity.ok().body(StatusLists(lists.map { URI.create(it) }))
    }

    override fun serveList(
        @PathVariable(value = "id") id: String,
        @RequestHeader headers: HttpHeaders,
    ): ResponseEntity<ByteArray> {
        val mediaType =
            headers.accept
                .filter { mt -> TokenFormat.entries.any { mt.includes(it.mediaType) } }
                .mediaTypeWithHighestPriority()

        val tokenFormat =
            if (mediaType == null || mediaType.includes(TokenFormat.JWT.mediaType)) {
                TokenFormat.JWT
            } else {
                TokenFormat.entries.firstOrNull { mediaType.includes(it.mediaType) }
                    ?: throw ErrorResponseException(
                        UNSUPPORTED_MEDIA_TYPE,
                        ErrorResponse(
                            ErrorCode.UNSUPPORTED_MEDIA_TYPE.name,
                            "Mediatype $mediaType is not supported",
                        ),
                    )
            }

        return serveList(id, headers, tokenFormat)
    }

    private fun serveList(
        id: String,
        headers: HttpHeaders,
        tokenFormat: TokenFormat,
    ): ResponseEntity<ByteArray> {
        val uuid =
            try {
                UUID.fromString(id)
            } catch (e: IllegalArgumentException) {
                throw ErrorResponseException(
                    NOT_FOUND,
                    ErrorResponse(ErrorCode.NO_SUCH_LIST.name, "No list with id $id"),
                )
            }

        // a var is used to capture the CacheEntry instead of the result from compute to prevent
        // immediate garbage collection after creating the SoftReference
        var computedEntry: CacheEntry? = null
        cache.compute(CacheKey(uuid, tokenFormat)) { key, currentValue ->
            val entry = currentValue?.get()
            if (currentValue == null || entry == null || !entry.isUpToDate()) {
                if (currentValue != null && entry == null) {
                    statsService.listCacheEvent(
                        "${config.publicUrl}/$uuid",
                        CACHE_SOFT_REFERENCE_CLEARED,
                    )
                }
                statsService.listCacheEvent("${config.publicUrl}/$uuid", CACHE_MISS)
                computedEntry = load(key)
                computedEntry?.let { SoftReference(it) }
            } else {
                statsService.listCacheEvent("${config.publicUrl}/$uuid", CACHE_HIT)
                entry.updateAccessed()
                computedEntry = entry
                currentValue
            }
        }

        val computedEntryVal = computedEntry

        if (computedEntryVal == null) {
            throw ErrorResponseException(
                NOT_FOUND,
                ErrorResponse(ErrorCode.NO_SUCH_LIST.name, "No list with id $id"),
            )
        } else {
            val eTag = computedEntryVal.created.toEpochMilli().toString()

            val responseHeaders = { responseHeaders: HttpHeaders ->
                responseHeaders.eTag = """"$eTag""""
                responseHeaders.setLastModified(computedEntryVal.created)
                responseHeaders.setCacheControl(CacheControl.noCache())
                responseHeaders.contentType = tokenFormat.mediaType
                responseHeaders.vary = listOf("Accept")
            }

            return if (modifiedAccordingToCacheHeaders(headers, eTag, computedEntryVal)) {
                ResponseEntity.ok().headers(responseHeaders).body(computedEntryVal.data)
            } else {
                ResponseEntity.status(HttpStatus.NOT_MODIFIED).headers(responseHeaders).build()
            }
        }
    }

    private fun modifiedAccordingToCacheHeaders(
        headers: HttpHeaders,
        eTag: String,
        cacheEntry: CacheEntry,
    ): Boolean {
        return if (headers.ifNoneMatch.isNotEmpty()) {
            headers.ifNoneMatch.none { it == eTag }
        } else if (headers.ifModifiedSince != IF_MODIFIED_SINCE_NOT_SET) {
            cacheEntry.created.isAfter(Instant.ofEpochMilli(headers.ifModifiedSince))
        } else {
            true
        }
    }

    private fun load(cacheKey: CacheKey): CacheEntry? {
        val data = statusListTokenSource.load(cacheKey.uuid, cacheKey.format)
        return data?.let {
            CacheEntry(
                cacheKey,
                it.data,
                it.lastModified,
                accessed = AtomicReference(Instant.now()),
            )
        }
    }

    data class CacheKey(val uuid: UUID, val format: TokenFormat)

    private inner class CacheEntry(
        val key: CacheKey,
        val data: ByteArray,
        val created: Instant,
        val accessed: AtomicReference<Instant>,
    ) {

        fun isUpToDate(): Boolean {
            return !created.isBefore(statusListTokenSource.lastModified(key.uuid, key.format))
        }

        fun hasBeenUsedRecently(): Boolean {
            return accessed.get().plus(config.cacheDuration).isAfter(Instant.now())
        }

        fun updateAccessed() {
            val now = Instant.now()
            accessed.updateAndGet { current ->
                if (current.isBefore(now)) {
                    now
                } else {
                    current
                }
            }
        }
    }
}

private fun List<MediaType>.mediaTypeWithHighestPriority(): MediaType? {
    val accept = this.toMutableList()
    accept.sortWith { o1, o2 ->
        when {
            o1.includes(o2) -> 1
            o2.includes(o1) -> -1
            else -> 0
        }
    }
    accept.sortByDescending { it.qualityValue }
    return accept.firstOrNull()
}
