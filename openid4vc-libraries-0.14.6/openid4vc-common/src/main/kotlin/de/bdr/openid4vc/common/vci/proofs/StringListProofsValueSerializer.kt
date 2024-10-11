/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci.proofs

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class StringProofsValueSerializer<T : Proof>(
    private val toString: (T) -> String,
    private val fromString: (String) -> T
) : KSerializer<Proof> {

    override val descriptor = PrimitiveSerialDescriptor("ProofString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) = fromString(decoder.decodeString())

    @SuppressWarnings("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: Proof) {
        encoder.encodeString(toString(value as T))
    }
}
