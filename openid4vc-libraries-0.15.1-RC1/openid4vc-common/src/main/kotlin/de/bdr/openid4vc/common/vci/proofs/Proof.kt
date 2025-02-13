/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
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

    val proofTypeConfigurationClass: KClass<out ProofTypeConfiguration>

    fun register() {
        ProofTypeRegistry.registry[value] = this
    }
}

@Serializable(with = ProofSerializer::class)
interface Proof {
    val proofType: ProofType
}
