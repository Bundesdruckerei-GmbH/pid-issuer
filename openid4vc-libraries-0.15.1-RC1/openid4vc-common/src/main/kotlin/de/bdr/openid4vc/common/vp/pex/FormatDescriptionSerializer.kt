/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.pex

import de.bdr.openid4vc.common.formats.CredentialFormatRegistry
import kotlin.reflect.full.createType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.mapSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeCollection
import kotlinx.serialization.serializer

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
                value,
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
                    as KSerializer<FormatDescription>,
            )
        compositeDecoder.endStructure(descriptor)
        return result
    }
}
