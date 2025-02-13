/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common

import com.nimbusds.jose.jwk.JWK
import java.text.ParseException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class JwkSerializer : KSerializer<JWK> {

    private val delegate = JsonElement.serializer()

    override val descriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: JWK) {
        delegate.serialize(encoder, value.toJSONObject().mapStructureToJson())
    }

    override fun deserialize(decoder: Decoder): JWK {
        val deserialized = delegate.deserialize(decoder)
        if (deserialized is JsonObject) {
            try {
                return JWK.parse(deserialized.jsonToMapStructure())
            } catch (e: ParseException) {
                throw SerializationException("Failed to parse JWK", e)
            }
        } else {
            throw SerializationException("JWK must be a encoded as json object")
        }
    }
}
