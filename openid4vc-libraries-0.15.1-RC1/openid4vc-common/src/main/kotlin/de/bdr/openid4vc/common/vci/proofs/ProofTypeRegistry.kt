/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci.proofs

import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProofType

internal object ProofTypeRegistry {

    val registry = mutableMapOf<String, ProofType>()

    init {
        JwtProofType.register()
    }
}
