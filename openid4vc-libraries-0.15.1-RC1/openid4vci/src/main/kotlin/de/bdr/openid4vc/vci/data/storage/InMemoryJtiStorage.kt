/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.vci.data.storage

import de.bdr.openid4vc.vci.logging.Oid4VcLog
import de.bdr.openid4vc.vci.utils.clock
import java.time.Instant
import java.util.Collections.synchronizedSet
import java.util.PriorityQueue

class InMemoryJtiStorage(private val capacity: Int = 10000) :
    de.bdr.openid4vc.vci.data.storage.JtiStorage {

    private val expiredQueue = PriorityQueue { a: Pair<String, Instant>, b: Pair<String, Instant> ->
        a.second.compareTo(b.second)
    }

    private val seen = synchronizedSet(HashSet<String>())

    override fun isUnused(jti: String, validUntil: Instant): Boolean {
        synchronized(expiredQueue) {
            removeExpired()
            if (expiredQueue.size >= capacity) {
                Oid4VcLog.log.warn("JTI $jti reported as used due to capacity restriction.")
                return false
            }
            val result = !seen.contains(jti)
            expiredQueue.add(Pair(jti, validUntil))
            seen.add(jti)
            return result
        }
    }

    private fun removeExpired() {
        val now = Instant.now(clock)
        var next = expiredQueue.peek()
        while (next != null && next.second.isBefore(now)) {
            expiredQueue.poll()
            seen.remove(next.first)
            next = expiredQueue.peek()
        }
    }
}
