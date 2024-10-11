/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vp

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.formats.CredentialFormatRegistry
import kotlin.reflect.full.createType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.mapSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeCollection
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

@Serializable
data class PresentationDefinition(
    @SerialName("id") val id: String,
    @SerialName("input_descriptors") val inputDescriptors: List<InputDescriptor>,
)

@Serializable
data class InputDescriptor(
    /* REQUIRED: Defined in DIF Presentation Exchange */
    @SerialName("id") val id: String,
    /* OPTIONAL: Defined in DIF Presentation Exchange */
    @Serializable(with = FormatDescriptionSerializer::class)
    @SerialName("format")
    val format: FormatDescription,
    /* OPTIONAL: Defined in DIF Presentation Exchange */
    @SerialName("constraints") val constraints: Constraints? = null,
)

@Serializable(with = FormatDescriptionSerializer::class)
interface FormatDescription {
    val type: CredentialFormat
}

class FormatDescriptionSerializer : KSerializer<FormatDescription> {

    private val keyDescriptor =
        PrimitiveSerialDescriptor("FormatDescriptionKey", PrimitiveKind.STRING)

    @OptIn(InternalSerializationApi::class)
    private val valueDescriptor =
        buildSerialDescriptor("FormatDescriptionSerializer", PolymorphicKind.SEALED)

    override val descriptor = mapSerialDescriptor(keyDescriptor, valueDescriptor)

    override fun serialize(encoder: Encoder, value: FormatDescription) {
        val type = value.type
        encoder.encodeCollection(descriptor, 2) {
            encodeStringElement(descriptor, 0, type.format)
            encodeSerializableElement(
                descriptor,
                1,
                serializer(type.formatDescriptionClass.createType()),
                value
            )
        }
    }

    override fun deserialize(decoder: Decoder): FormatDescription {
        val compositeDecoder = decoder.beginStructure(descriptor)
        var index = compositeDecoder.decodeElementIndex(descriptor)
        val format = compositeDecoder.decodeStringElement(descriptor, index)
        val type =
            CredentialFormatRegistry.registry[format] ?: error("Invalid credential format $format")
        index = compositeDecoder.decodeElementIndex(descriptor)
        val result =
            compositeDecoder.decodeSerializableElement(
                descriptor,
                index,
                serializer(type.formatDescriptionClass.createType())
                    as KSerializer<FormatDescription>
            )
        compositeDecoder.endStructure(descriptor)
        return result
    }
}

@Serializable
data class Constraints(
    @SerialName("fields") val fields: List<Field>,
    @SerialName("limit_disclosure") val limitDisclosure: String? = null,
)

@Serializable
data class Field(
    @SerialName("path") val path: List<String>,
    @SerialName("id") val id: String? = null,
    @SerialName("purpose") val purpose: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("filter") val filter: JsonObject? = null,
    @SerialName("optional") val optional: Boolean? = null,
    @SerialName("intent_to_retain") val intentToRetain: Boolean? = null,
)
