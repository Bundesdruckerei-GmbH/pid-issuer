/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci.proofs

import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException.ReasonCode.INVALID_PROOF
import kotlin.reflect.full.createType
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer

class ProofSerializer : JsonContentPolymorphicSerializer<Proof>(Proof::class) {
    @OptIn(ExperimentalSerializationApi::class)
    @SuppressWarnings("UNCHECKED_CAST")
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Proof> {
        val proofType =
            element.jsonObject["proof_type"]
                ?: throw MissingFieldException("proof_type", descriptor.serialName)
        val proofTypeValue =
            (proofType as? JsonPrimitive
                    ?: throw SerializationException("Invalid proof_type $proofType"))
                .content
        val proofClass =
            ProofTypeRegistry.registry[proofTypeValue]?.proofClass
                ?: throw SpecificIllegalArgumentException(
                    INVALID_PROOF,
                    "Invalid proof_type $proofTypeValue"
                )

        return serializer(proofClass.createType()) as DeserializationStrategy<Proof>
    }
}
