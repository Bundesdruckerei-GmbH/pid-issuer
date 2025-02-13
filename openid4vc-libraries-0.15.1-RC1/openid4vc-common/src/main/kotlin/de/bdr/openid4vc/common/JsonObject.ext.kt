/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common

import java.lang.IllegalArgumentException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@SuppressWarnings("UNCHECKED_CAST")
fun Any?.mapStructureToJson(): JsonElement {
    return when (this) {
        null -> JsonNull
        is Map<*, *> -> (this as Map<String, Any?>).mapStructureToJson()
        is Iterable<*> -> (this as Iterable<Any?>).mapStructureToJson()
        is Boolean -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        else ->
            throw IllegalArgumentException(
                "Unexpected json value in map structure. Type: ${this::class.java.name}"
            )
    }
}

fun Map<String, Any?>.mapStructureToJson(): JsonObject {
    return JsonObject(mapValues { it.value.mapStructureToJson() })
}

fun Iterable<Any?>.mapStructureToJson(): JsonArray {
    return JsonArray(map { it.mapStructureToJson() })
}

fun JsonElement.jsonToMapStructure(): Any? {
    return when (this) {
        is JsonNull -> jsonToMapStructure()
        is JsonPrimitive -> jsonToMapStructure()
        is JsonArray -> jsonToMapStructure()
        is JsonObject -> jsonToMapStructure()
    }
}

fun JsonObject.jsonToMapStructure(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    forEach { (key, value) -> map[key] = value.jsonToMapStructure() }
    return map
}

fun JsonArray.jsonToMapStructure(): Any? {
    val list = mutableListOf<Any?>()
    forEach { list.add(it.jsonToMapStructure()) }
    return list
}

fun JsonNull.jsonToMapStructure(): Any? = null

fun JsonPrimitive.jsonToMapStructure(): Any {
    if (isString) return content
    if (content == "true") return true
    if (content == "false") return false
    return tryConversion(
        content,
        { it.toInt() },
        { it.toLong() },
        { it.toBigInteger() },
        { it.toFloatValueExact() },
        { it.toDoubleValueExact() },
        { it.toBigDecimal() }
    )
}

private fun String.toFloatValueExact(): Float {
    val float = toFloat()
    if (float.toString() == this) {
        return float
    } else {
        throw IllegalArgumentException("Value can not be converted to float without loss")
    }
}

private fun String.toDoubleValueExact(): Double {
    val double = toDouble()
    if (double.toString() == this) {
        return double
    } else {
        throw IllegalArgumentException("Value can not be converted to double without loss")
    }
}

private fun tryConversion(content: String, vararg converters: (String) -> Any): Any {
    converters.forEach {
        try {
            return it(content)
        } catch (e: Throwable) {
            // continue loop
        }
    }
    throw IllegalArgumentException("Non convertible primitive value $content")
}
