/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.dcql

import isNonNegativeInteger
import java.lang.StringBuilder
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer

fun Iterable<String>.toJsonPointer(): String {
    val result = StringBuilder()
    forEach {
        val escapedSegment = it.replace("~", "~0").replace("/", "~1")
        result.append('/').append(escapedSegment)
    }
    return result.toString()
}

fun ClaimsPathPointer(vararg selectors: ClaimsPathPointerSelector): ClaimsPathPointer {
    val list = selectors.toList()
    val distinct = list.filterIsInstance<DistinctClaimsPathPointerSelector>()
    return if (distinct.size == list.size) {
        DistinctClaimsPathPointer(distinct)
    } else {
        ClaimsPathPointer(list)
    }
}

@Serializable(with = ClaimsPathPointerSerializer::class)
open class ClaimsPathPointer
internal constructor(open val selectors: List<ClaimsPathPointerSelector>) {

    fun resolve(root: JsonElement): Set<JsonElement> =
        resolveWithDistinctPaths(root).mapTo(mutableSetOf()) { it.jsonElement }

    fun resolveWithDistinctPaths(root: JsonElement): Set<JsonElementWithDistinctPath> {
        var result =
            listOf(Pair<JsonElement, List<DistinctClaimsPathPointerSelector>>(root, emptyList()))
                .asSequence()
        selectors.forEach { selector -> result = selector.apply(result) }
        return result
            .map { JsonElementWithDistinctPath(it.first, DistinctClaimsPathPointer(it.second)) }
            .toSet()
    }

    override fun toString() = Json.encodeToString(this)

    override fun hashCode() = 584732474 + selectors.hashCode()

    override fun equals(other: Any?) =
        other === this || other is ClaimsPathPointer && other.selectors == selectors
}

@Serializable(with = ClaimsPathPointerSelectorSerializer::class)
sealed interface ClaimsPathPointerSelector {
    fun apply(
        current: Sequence<Pair<JsonElement, List<DistinctClaimsPathPointerSelector>>>
    ): Sequence<Pair<JsonElement, List<DistinctClaimsPathPointerSelector>>>
}

@Serializable(with = ArrayElementSelectorSerializer::class)
data class ArrayElementSelector(val index: Int) : DistinctClaimsPathPointerSelector {
    override fun apply(
        current: Sequence<Pair<JsonElement, List<DistinctClaimsPathPointerSelector>>>
    ) =
        current.mapNotNull {
            val jsonElement = it.first
            if (jsonElement !is JsonArray)
                throw ClaimsPathPointerProcessingException(
                    "Error matching path, value not an array",
                    ClaimsPathPointer(it.second),
                )
            if (index < jsonElement.size) {
                Pair(jsonElement[index], it.second.and(this@ArrayElementSelector))
            } else {
                null
            }
        }

    override fun toString() = index.toString()
}

@Serializable(with = AllArrayElementsSelectorSerializer::class)
data object AllArrayElementsSelector : ClaimsPathPointerSelector {
    override fun apply(
        current: Sequence<Pair<JsonElement, List<DistinctClaimsPathPointerSelector>>>
    ) =
        current.flatMap { currentElement ->
            val jsonElement = currentElement.first
            if (jsonElement !is JsonArray)
                throw ClaimsPathPointerProcessingException(
                    "Error matching path, value not an array",
                    ClaimsPathPointer(currentElement.second),
                )
            jsonElement
                .mapIndexed { index, arrayElement ->
                    Pair(arrayElement, currentElement.second.and(ArrayElementSelector(index)))
                }
                .asSequence()
        }

    override fun toString() = "*"
}

@Serializable(with = ObjectElementSelectorSerializer::class)
data class ObjectElementSelector(val claimName: String) : DistinctClaimsPathPointerSelector {
    override fun apply(
        current: Sequence<Pair<JsonElement, List<DistinctClaimsPathPointerSelector>>>
    ) =
        current.mapNotNull {
            val jsonElement = it.first
            if (jsonElement !is JsonObject)
                throw ClaimsPathPointerProcessingException(
                    "Error matching path, value not an object",
                    ClaimsPathPointer(it.second),
                )
            jsonElement[claimName]?.let { objectElement ->
                Pair(objectElement, it.second.and(this@ObjectElementSelector))
            }
        }

    override fun toString() = claimName
}

class ClaimsPathPointerProcessingException(
    message: String,
    val claimsPathPointer: ClaimsPathPointer,
) : Exception("$message: $claimsPathPointer")

// serialization

object ClaimsPathPointerSerializer : KSerializer<ClaimsPathPointer> {

    private val delegate = serializer<Array<ClaimsPathPointerSelector>>()

    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): ClaimsPathPointer {
        return ClaimsPathPointer(*delegate.deserialize(decoder))
    }

    override fun serialize(encoder: Encoder, value: ClaimsPathPointer) {
        delegate.serialize(encoder, value.selectors.toTypedArray())
    }
}

object ClaimsPathPointerSelectorSerializer :
    JsonContentPolymorphicSerializer<ClaimsPathPointerSelector>(ClaimsPathPointerSelector::class) {
    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<ClaimsPathPointerSelector> {
        if (element !is JsonPrimitive)
            throw SerializationException(
                "Element of claims path pointer must be a string, non negative integer or null"
            )
        return when {
            element.isString -> ObjectElementSelectorSerializer
            element.isNonNegativeInteger -> ArrayElementSelectorSerializer
            element is JsonNull -> AllArrayElementsSelectorSerializer
            else ->
                throw SerializationException(
                    "Element of claims path pointer must be a string, non negative integer or null"
                )
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
object AllArrayElementsSelectorSerializer : KSerializer<AllArrayElementsSelector> {

    override val descriptor =
        PrimitiveSerialDescriptor("AllArrayElementsSelectorSerializer", PrimitiveKind.INT).nullable

    override fun deserialize(decoder: Decoder): AllArrayElementsSelector {
        decoder.decodeNull()
        return AllArrayElementsSelector
    }

    override fun serialize(encoder: Encoder, value: AllArrayElementsSelector) = encoder.encodeNull()
}

object ArrayElementSelectorSerializer : KSerializer<ArrayElementSelector> {

    private val delegate = Int.serializer()

    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder) = ArrayElementSelector(delegate.deserialize(decoder))

    override fun serialize(encoder: Encoder, value: ArrayElementSelector) =
        delegate.serialize(encoder, value.index)
}

object ObjectElementSelectorSerializer : KSerializer<ObjectElementSelector> {

    private val delegate = String.serializer()

    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder) =
        ObjectElementSelector(delegate.deserialize(decoder))

    override fun serialize(encoder: Encoder, value: ObjectElementSelector) =
        delegate.serialize(encoder, value.claimName)
}

internal fun List<DistinctClaimsPathPointerSelector>.and(
    next: DistinctClaimsPathPointerSelector
): List<DistinctClaimsPathPointerSelector> {
    val list = ArrayList<DistinctClaimsPathPointerSelector>(this.size + 1)
    list.addAll(this)
    list.add(next)
    return list
}
