/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.statuslist

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import java.net.URI
import java.net.http.HttpClient
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** A status list loader that has caching capabilities. */
class CachingCapableHttpStatusListSource(

    /** The verifier to use to verify the status list tokens. */
    private val verifier: Verifier,

    /** Defines if status lists will be cached according to ttl and exp time. */
    useCache: Boolean,

    /**
     * The maximum size of the cached status lists in bytes.
     *
     * This only measures the size of the bitstrings from the status lists. For each status list
     * some additional memory is required to store the objects and some metadata.
     *
     * If the size constraint is violated entries will be evicted based on last access and access
     * frequency.
     */
    maxSize: Long = Long.MAX_VALUE,

    /** The httpTimeout to use when loading status lists. */
    private val httpTimeout: Duration = Duration.ofSeconds(5),

    /** The clock to use to compare for ttl and exp. */
    private val clock: Clock = Clock.systemDefaultZone(),

    /** The http client used to fetch the tokens. */
    private var client: HttpClient = defaultHttpClient
) : StatusListSource {

    private val entryExpiry =
        object : Expiry<URI, Entry> {
            override fun expireAfterCreate(key: URI, value: Entry, currentTime: Long) =
                Duration.between(clock.instant(), value.expiresAt).toNanos()

            override fun expireAfterUpdate(
                key: URI,
                value: Entry,
                currentTime: Long,
                currentDuration: Long
            ) = Duration.between(clock.instant(), value.expiresAt).toNanos()

            override fun expireAfterRead(
                key: URI,
                value: Entry,
                currentTime: Long,
                currentDuration: Long
            ) = Long.MAX_VALUE
        }

    private val cache =
        if (useCache) {
            Caffeine.newBuilder()
                .weigher { _: URI, value: Entry -> value.size }
                .maximumSize(maxSize)
                .ticker(clock.ticker())
                .expireAfter(entryExpiry)
                .build<URI, Entry>(::loadEntry)
        } else {
            null
        }

    private inner class Entry(statusListToken: StatusListToken) {
        val statusList = statusListToken.statusList
        val expiresAt: Instant =
            min(statusListToken.ttl?.let { clock.instant().plus(it) }, statusListToken.expiresAt)
        val size = statusList.getList().size
    }

    /** Gets a status list by URI using the cache if configured. */
    override fun get(statusListUri: URI): StatusList {
        return if (cache != null) {
            return cache[statusListUri].statusList
        } else {
            loadEntry(statusListUri).statusList
        }
    }

    /**
     * Invalidates the entry for `statusListUri` in the cache if a cache is configured.
     *
     * @throws IllegalStateException if no cache is configured
     */
    fun invalidateCache(statusListUri: URI) {
        cache?.invalidate(statusListUri) ?: error("Cache not used")
    }

    /**
     * Invalidates the cache if configured.
     *
     * @throws IllegalStateException if no cache is configured
     */
    fun invalidateCache() {
        cache?.invalidateAll() ?: error("Cache not used")
    }

    private fun loadEntry(statusListUri: URI): Entry {
        val token = StatusListToken.fetch(statusListUri, client)
        requireNotNull(token.jwt) { "Failed to fetch token as JWT" }
        token.jwt.verify(verifier.verifier(token.jwt))
        return Entry(token)
    }
}
