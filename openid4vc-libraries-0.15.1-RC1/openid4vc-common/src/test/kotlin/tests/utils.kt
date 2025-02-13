/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package tests

import de.bdr.openid4vc.common.clock
import java.time.Clock
import java.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified T> Json.encodeAndDecodeFromString(value: T) =
    Json.decodeFromString<T>(Json.encodeToString(value))

fun <T> withFixedTime(time: Instant = clock.instant(), code: FixedTimeScope.() -> T): T {
    val scope =
        object : FixedTimeScope {
            override var fixedTime: Instant = time
        }
    clock = Clock.fixed(time, clock.zone)
    try {
        return scope.code()
    } finally {
        clock = Clock.systemDefaultZone()
    }
}

interface FixedTimeScope {
    var fixedTime: Instant
}
