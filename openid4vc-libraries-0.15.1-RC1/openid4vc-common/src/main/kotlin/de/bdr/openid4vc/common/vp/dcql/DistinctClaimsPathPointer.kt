/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.dcql

import isNonNegativeInteger
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer

@Serializable(with = DistinctClaimsPathPointerSelectorSerializer::class)
sealed interface DistinctClaimsPathPointerSelector : ClaimsPathPointerSelector

@Serializable(with = DistinctClaimsPathPointerSerializer::class)
class DistinctClaimsPathPointer(override val selectors: List<DistinctClaimsPathPointerSelector>) :
    ClaimsPathPointer(selectors) {
    /** Transforms this instance to a JSON Pointer according to RFC 6901 */
    fun toJsonPointer() =
        selectors
            .map {
                when (it) {
                    is ArrayElementSelector -> it.index.toString()
                    is ObjectElementSelector -> it.claimName
                }
            }
            .toJsonPointer()

    fun startsWith(other: DistinctClaimsPathPointer): Boolean {
        if (other.selectors.size > selectors.size) return false
        other.selectors.forEachIndexed { index, otherSelector ->
            if (selectors[index] != otherSelector) return false
        }
        return true
    }

    fun isChildOf(parent: DistinctClaimsPathPointer): Boolean {
        return parent.selectors.size < selectors.size && startsWith(parent)
    }

    fun parent(): DistinctClaimsPathPointer? {
        return if (selectors.isEmpty()) {
            null
        } else {
            DistinctClaimsPathPointer(selectors.dropLast(1))
        }
    }

    fun objectElement(name: String) =
        DistinctClaimsPathPointer(selectors.and(ObjectElementSelector(name)))

    fun arrayElement(index: Int): DistinctClaimsPathPointer {
        require(index >= 0) { "Index must not be negative" }
        return DistinctClaimsPathPointer(selectors.and(ArrayElementSelector(index)))
    }

    override fun toString() = toJsonPointer()

    companion object {
        val ROOT = DistinctClaimsPathPointer(emptyList())

        /**
         * Parses a JSON pointer to a DistinctClaimsPathPointer.
         *
         * Because in a [ClaimsPathPointer] it is always known if a selector references an object or
         * an array, but this is not the case for JSON pointer the json object the JSON pointer
         * needs to be applied to must be provided to construct it.
         */
        fun fromJsonPointer(jsonPointer: String, json: JsonObject): DistinctClaimsPathPointer {
            if (jsonPointer.isEmpty()) return ROOT
            require(jsonPointer.startsWith("/")) { "JSON Pointer must be empty or start with a /" }
            var current: JsonElement = json
            val selectors =
                jsonPointer.substring(1).split("/").map {
                    val segment = it.replace("~1", "/").replace("~0", "~")
                    val asInt = segment.toIntOrNull()
                    if (asInt != null && asInt > 1 && current is JsonArray) {
                        current = (current as JsonArray)[asInt]
                        ArrayElementSelector(asInt)
                    } else {
                        current =
                            (json as? JsonObject)?.get(segment)
                                ?: throw IllegalArgumentException(
                                    "JSON pointer can not be resolved in provided json object"
                                )
                        ObjectElementSelector(segment)
                    }
                }
            return DistinctClaimsPathPointer(selectors)
        }
    }
}

// serialization

object DistinctClaimsPathPointerSerializer : KSerializer<DistinctClaimsPathPointer> {

    private val delegate = serializer<Array<DistinctClaimsPathPointerSelector>>()

    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): DistinctClaimsPathPointer {
        return DistinctClaimsPathPointer(delegate.deserialize(decoder).toList())
    }

    override fun serialize(encoder: Encoder, value: DistinctClaimsPathPointer) {
        delegate.serialize(encoder, value.selectors.toTypedArray())
    }
}

object DistinctClaimsPathPointerSelectorSerializer :
    JsonContentPolymorphicSerializer<DistinctClaimsPathPointerSelector>(
        DistinctClaimsPathPointerSelector::class
    ) {
    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<DistinctClaimsPathPointerSelector> {
        if (element !is JsonPrimitive)
            throw SerializationException(
                "Element of distinct claims path pointer must be a string or non negative integer"
            )
        return when {
            element.isString -> ObjectElementSelectorSerializer
            element.isNonNegativeInteger -> ArrayElementSelectorSerializer
            else ->
                throw SerializationException(
                    "Element of distinct claims path pointer must be a string or non negative integer"
                )
        }
    }
}
