/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** @return The string content of this JsonElement if it is a JsonString, otherwise `null`. */
fun JsonElement.jsonStringContent(): String? =
    when {
        this is JsonPrimitive && isString -> this.content
        else -> null
    }

val JsonElement.isNonNegativeInteger: Boolean
    get() {
        if (this !is JsonPrimitive) {
            return false
        }
        val intOrNull = intOrNull
        return intOrNull != null && intOrNull >= 0
    }
