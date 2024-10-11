/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.signing

import com.nimbusds.jose.jwk.JWK

class JwkSigner(private val jwk: JWK) : JcaKeyBasedSigner() {

    override val keys = JwkKeyMaterial(jwk.toPublicJWK())

    override val supportedAlgorithm = algorithm()

    private fun algorithm(): SupportedAlgorithm {
        val curve = jwk.toECKey().curve
        return SupportedAlgorithm.entries.firstOrNull { it.curve == curve }
            ?: throw IllegalArgumentException("Unsupported curve $curve")
    }

    override val privateKey = jwk.toECKey().toECPrivateKey()
}
