/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.signing

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.util.Base64URL
import de.bdr.openid4vc.common.Algorithm
import de.bdr.openid4vc.common.JWSAlgorithms

class SignerToJwsSignerAdapter(private val signer: Signer) : JWSSigner {

    val jwsAlgorithm =
        when (signer.algorithm) {
            Algorithm.ES256 -> JWSAlgorithm.ES256
            Algorithm.ES384 -> JWSAlgorithm.ES384
            Algorithm.ES512 -> JWSAlgorithm.ES512
            Algorithm.DVS_P256_SHA256_HS256 -> JWSAlgorithms.DVS_P256_SHA256_HS256
        }

    override fun getJCAContext() = JCAContext()

    override fun supportedJWSAlgorithms() = setOf(jwsAlgorithm)

    override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
        val raw = signer.sign(signingInput)
        val transcoded =
            if (signer.algorithm == Algorithm.DVS_P256_SHA256_HS256) raw
            else transcodeSignature(raw)
        return Base64URL.encode(transcoded)
    }

    private fun transcodeSignature(raw: ByteArray): ByteArray {
        val rsByteArrayLength = ECDSA.getSignatureByteArrayLength(jwsAlgorithm)
        return ECDSA.transcodeSignatureToConcat(raw, rsByteArrayLength)
    }
}
