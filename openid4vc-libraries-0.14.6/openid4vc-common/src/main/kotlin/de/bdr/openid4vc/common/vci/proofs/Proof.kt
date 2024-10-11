/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci.proofs

import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable(with = ProofTypeSerializer::class)
interface ProofType {

    val proofsValueSerializer: KSerializer<Proof>
    val value: String
    val proofClass: KClass<out Proof>

    fun register() {
        ProofTypeRegistry.registry[value] = this
    }
}

@Serializable(with = ProofSerializer::class)
interface Proof {
    val proofType: ProofType
}
