/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.dcql

import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException
import de.bdr.openid4vc.common.formats.CredentialFormatRegistry
import kotlin.reflect.full.createType
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer

class CredentialQuerySerializer :
    JsonContentPolymorphicSerializer<CredentialQuery>(CredentialQuery::class) {
    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<CredentialQuery> {
        val format = element.jsonObject["format"] ?: throw SerializationException("Missing format")
        val formatString =
            (format as? JsonPrimitive)?.content
                ?: throw SerializationException("format of CredentialRequest must be a string")
        val requestClass =
            (CredentialFormatRegistry.registry[formatString]?.credentialQueryClass
                ?: throw SpecificIllegalArgumentException(
                    SpecificIllegalArgumentException.ReasonCode.INVALID_CREDENTIAL_FORMAT,
                    "CredentialRequest format $format unknown",
                ))
        return serializer(requestClass.createType()) as DeserializationStrategy<CredentialQuery>
    }
}
