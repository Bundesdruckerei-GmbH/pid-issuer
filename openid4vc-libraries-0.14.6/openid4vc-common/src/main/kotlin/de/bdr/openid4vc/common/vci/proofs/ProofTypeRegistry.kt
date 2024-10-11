/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci.proofs

import de.bdr.openid4vc.common.vci.proofs.cwt.CwtProofType
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProofType

internal object ProofTypeRegistry {

    val registry = mutableMapOf<String, ProofType>()

    init {
        JwtProofType.register()
        CwtProofType.register()
    }
}
