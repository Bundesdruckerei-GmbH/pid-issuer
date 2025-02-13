/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.statuslist

import com.github.benmanes.caffeine.cache.Ticker
import java.time.Clock
import java.time.Instant

internal fun Clock.ticker() = Ticker {
    val instant = this.instant()
    instant.toEpochMilli() * 1_000_000 + instant.nano
}

/**
 * Returns the earliest instant in the argument list.
 *
 * Individual values may be `null` which means they will be ignored.
 *
 * If no value is passed or all values are `null` then `Instant.MAX` will be returned.
 */
internal fun min(vararg instants: Instant?): Instant {
    var result = Instant.MAX
    instants.forEach {
        if (it != null && it.isBefore(result)) {
            result = it
        }
    }
    return result
}
