/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.signing

import com.nimbusds.jose.jwk.Curve
import java.security.PrivateKey
import java.security.Signature

abstract class JcaKeyBasedSigner : Signer {

    protected abstract val privateKey: PrivateKey

    protected abstract val supportedAlgorithm: SupportedAlgorithm

    override val algorithm
        get() = supportedAlgorithm.algorithm

    override fun sign(data: ByteArray) = sign(supportedAlgorithm.jcaAlgorithm, data)

    private fun sign(jcaAlgorithm: String, data: ByteArray): ByteArray {
        val signature = Signature.getInstance(jcaAlgorithm)
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }

    enum class SupportedAlgorithm(
        val curve: Curve,
        val algorithm: de.bdr.openid4vc.common.Algorithm,
        val jcaAlgorithm: String
    ) {
        ES256(Curve.P_256, de.bdr.openid4vc.common.Algorithm.ES256, "SHA256WithECDSA"),
        ES384(Curve.P_384, de.bdr.openid4vc.common.Algorithm.ES384, "SHA384WithECDSA"),
        ES512(Curve.P_521, de.bdr.openid4vc.common.Algorithm.ES512, "SHA512WithECDSA")
    }
}
