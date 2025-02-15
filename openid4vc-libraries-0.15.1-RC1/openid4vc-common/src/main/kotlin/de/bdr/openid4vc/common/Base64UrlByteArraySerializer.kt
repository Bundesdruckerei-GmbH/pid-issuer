/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common

import java.util.Base64
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

object Base64UrlByteArraySerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Base64UrlByteArray", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray) {
        val base64UrlString = Base64.getUrlEncoder().encodeToString(value)
        encoder.encodeString(base64UrlString)
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        val base64UrlString = decoder.decodeString()
        return Base64.getUrlDecoder().decode(base64UrlString)
    }
}
