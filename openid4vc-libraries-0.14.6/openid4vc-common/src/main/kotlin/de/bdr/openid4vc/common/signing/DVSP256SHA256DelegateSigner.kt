/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.signing

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import de.bdr.openid4vc.common.Algorithm
import de.bdr.openid4vc.common.JWSAlgorithms.DVS_P256_SHA256_HS256
import de.bdr.openid4vc.common.signing.nimbus.DVSP256SHA256HS256MacSigner

class DVSP256SHA256HS256DelegateSigner(val jWSSigner: DVSP256SHA256HS256MacSigner) : Signer {

    override val algorithm: Algorithm
        get() = Algorithm.DVS_P256_SHA256_HS256

    override val keys: KeyMaterial by lazy {
        JwkKeyMaterial(ECKey.Builder(Curve.P_256, jWSSigner.dvsp256SHA256Key.pkR).build())
    }

    override fun sign(data: ByteArray): ByteArray {
        try {
            return jWSSigner.sign(JWSHeader.Builder(DVS_P256_SHA256_HS256).build(), data).decode()
        } catch (e: JOSEException) {
            throw RuntimeException(e)
        }
    }
}
