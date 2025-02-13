/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package tests

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import de.bdr.openid4vc.common.signing.JwkSigner
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProof

object TestData {
    fun loadTestdata(name: String) = javaClass.getResource("/testdata/$name").readText()

    fun jwtProof(): JwtProof {
        val key = ECKeyGenerator(Curve.P_256).generate()
        val signer = JwkSigner(key)
        return JwtProof.create(
            clientId = "clientId",
            audience = "audience",
            nonce = "nonce",
            signer = signer
        )
    }
}
