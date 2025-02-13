/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.vci.data.storage

import de.bdr.openid4vc.vci.logging.Oid4VcLog
import java.time.Duration
import java.time.Instant

class LimitedMapStorage<K : Any, V : Any>(
    private val name: String,
    private val limit: Int = 10000,
    private val maxAge: Duration = Duration.ofMinutes(60)
) {

    private val map = HashMap<K, ExpiringValue>()

    operator fun set(key: K, value: V) {
        cleanup()
        synchronized(map) { map[key] = ExpiringValue(value) }
    }

    operator fun get(key: K): V? {
        val expiringValue = synchronized(map) { map[key] }
        return if (expiringValue == null || expiringValue.expired) {
            null
        } else {
            expiringValue.value
        }
    }

    fun remove(key: K) {
        synchronized(map) { map.remove(key) }
    }

    fun find(test: (value: V) -> Boolean) =
        synchronized(map) { map.values.find { !it.expired && test(it.value) } }?.value

    private fun cleanup() {
        synchronized(map) {
            val iter = map.iterator()
            while (iter.hasNext()) {
                if (iter.next().value.expired) {
                    iter.remove()
                }
            }

            if (map.size > limit) {
                Oid4VcLog.log.warn(
                    "LimitedMapStorage $name size exceeded, removing ${map.size - limit} entries"
                )
                map.entries
                    .sortedBy { it.value.created }
                    .take(map.size - limit)
                    .forEach { map.remove(it.key) }
            }
        }
    }

    private inner class ExpiringValue(val value: V) {
        val created: Instant = Instant.now()
        val expired: Boolean
            get() = created.plus(maxAge).isBefore(Instant.now())
    }
}
