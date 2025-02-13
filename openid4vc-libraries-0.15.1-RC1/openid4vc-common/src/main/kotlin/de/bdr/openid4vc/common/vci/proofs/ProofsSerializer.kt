/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci.proofs

import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException.ReasonCode.INVALID_PROOF
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.descriptors.mapSerialDescriptor
import kotlinx.serialization.encoding.*

class ProofsSerializer : KSerializer<List<Proof>> {

    private val keyDescriptor = PrimitiveSerialDescriptor("ProofsKey", PrimitiveKind.STRING)

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    private val valueDescriptor =
        listSerialDescriptor(buildSerialDescriptor("ProofsSerializer", PolymorphicKind.SEALED))

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = mapSerialDescriptor(keyDescriptor, valueDescriptor)

    override fun deserialize(decoder: Decoder): List<Proof> {
        val compositeDecoder = decoder.beginStructure(descriptor)
        var index = compositeDecoder.decodeElementIndex(descriptor)

        if (index == CompositeDecoder.DECODE_DONE) {
            throw SpecificIllegalArgumentException(INVALID_PROOF, "Key proof list not present")
        }

        val proofTypeValue = compositeDecoder.decodeStringElement(descriptor, index)
        val proofType =
            ProofTypeRegistry.registry[proofTypeValue]
                ?: throw SpecificIllegalArgumentException(
                    INVALID_PROOF,
                    "Invalid proof_type $proofTypeValue",
                )
        index = compositeDecoder.decodeElementIndex(descriptor)
        val result =
            compositeDecoder.decodeSerializableElement(
                descriptor,
                index,
                ListSerializer(proofType.proofsValueSerializer),
            )
        compositeDecoder.endStructure(descriptor)
        return result
    }

    override fun serialize(encoder: Encoder, value: List<Proof>) {
        val proofType = value.first().proofType
        encoder.encodeCollection(descriptor, 2) {
            encodeStringElement(descriptor, 0, proofType.value)
            encodeSerializableElement(
                descriptor,
                1,
                ListSerializer(proofType.proofsValueSerializer),
                value,
            )
        }
    }
}
