/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.common.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate

object LocalDateFullDateSerializer : KSerializer<LocalDate> {
    // CBor tag #6.1004 (tstr) for the default FORMAT pattern: "yyyy-MM-dd"
    const val FULL_DATE_TAG = 1004uL

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(
            "java.time.LocalDate",
            PrimitiveKind.STRING
        )

    override fun serialize(encoder: Encoder, value: LocalDate) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}
