/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci.proofs

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class ProofTypeSerializer : KSerializer<ProofType> {

    override val descriptor =
        PrimitiveSerialDescriptor("de.bdr.openid4vc.common.vci.proofs.ProofTypeSerializer", STRING)

    override fun serialize(encoder: Encoder, value: ProofType) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ProofType {
        val proofType = decoder.decodeString()
        return ProofTypeRegistry.registry[proofType] ?: error("Unknown proof type $proofType")
    }
}
