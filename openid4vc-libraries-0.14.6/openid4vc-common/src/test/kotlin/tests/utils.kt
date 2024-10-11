/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package tests

import de.bdr.openid4vc.common.currentTimeMillis
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified T> Json.encodeAndDecodeFromString(value: T) =
    Json.decodeFromString<T>(Json.encodeToString(value))

fun <T> withFixedTime(
    timeInMilliseconds: Long = System.currentTimeMillis(),
    code: FixedTimeScope.() -> T
): T {
    val scope =
        object : FixedTimeScope {
            override var fixedTime: Long = timeInMilliseconds
        }
    currentTimeMillis = { scope.fixedTime }
    try {
        return scope.code()
    } finally {
        currentTimeMillis = System::currentTimeMillis
    }
}

interface FixedTimeScope {
    var fixedTime: Long
}
