/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci.proofs.jwt

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import de.bdr.openid4vc.common.Algorithm
import de.bdr.openid4vc.common.RobustJsonStringSerializer
import de.bdr.openid4vc.common.currentTimeMillis
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException.ReasonCode.INVALID_PROOF
import de.bdr.openid4vc.common.signing.Signer
import de.bdr.openid4vc.common.signing.SignerToJwsSignerAdapter
import de.bdr.openid4vc.common.vci.NonceService
import de.bdr.openid4vc.common.vci.NonceService.StandardNoncePurpose.C_NONCE
import de.bdr.openid4vc.common.vci.proofs.Proof
import de.bdr.openid4vc.common.vci.proofs.ProofType
import de.bdr.openid4vc.common.vci.proofs.ProofTypeConfiguration
import de.bdr.openid4vc.common.vci.proofs.StringProofsValueSerializer
import java.util.*
import kotlinx.serialization.*
import kotlinx.serialization.Transient

object JwtProofType : ProofType {
    override val value = "jwt"
    override val proofClass = JwtProof::class
    override val proofsValueSerializer = StringProofsValueSerializer(JwtProof::jwt, ::JwtProof)
    override val proofTypeConfigurationClass = JwtProofTypeConfiguration::class
}

@Serializable
data class JwtProofTypeConfiguration(
    @SerialName("proof_signing_alg_values_supported")
    override val signingAlgValuesSupported:
        List<@Serializable(RobustJsonStringSerializer::class) String>
) : ProofTypeConfiguration {
    override val proofType: ProofType = JwtProofType
}

@Serializable
data class JwtProof
@OptIn(ExperimentalSerializationApi::class)
constructor(
    val jwt: String,
    @SerialName("proof_type") @EncodeDefault override val proofType: ProofType = JwtProofType,
) : Proof {

    init {
        require(proofType == JwtProofType) { "Proof type must be JwtProofType" }
    }

    companion object {

        val JWT_TYPE = "openid4vci-proof+jwt"

        fun create(
            clientId: String?,
            audience: String,
            nonce: String,
            signer: Signer,
            issueTimestamp: Long = currentTimeMillis(),
        ): JwtProof {

            val jwk = signer.keys.jwk

            val header =
                JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(JOSEObjectType(JWT_TYPE))
                    .jwk(jwk)
                    .build()

            val claims =
                JWTClaimsSet.Builder()
                    .issuer(clientId)
                    .audience(audience)
                    .claim("nonce", nonce)
                    .issueTime(Date(issueTimestamp))
                    .build()

            val jwt = SignedJWT(header, claims)
            jwt.sign(SignerToJwsSignerAdapter(signer))

            return JwtProof(jwt.serialize())
        }
    }

    @Transient
    val signedJwt =
        try {
            SignedJWT.parse(jwt)
        } catch (e: Exception) {
            throw SpecificIllegalArgumentException(INVALID_PROOF, "Failed to parse jwt", e)
        }

    fun validate(
        clientId: String?,
        audience: String,
        nonceService: NonceService,
        proofIssueTimeToleranceMilliseconds: Long = 30_000,
    ): JWK {
        check(signedJwt.header.type.type == JWT_TYPE) { "Invalid proof type" }

        check(Algorithm.entries.any { it.jwsAlgorithm == signedJwt.header.algorithm }) {
            "Invalid algorithm"
        }

        check(signedJwt.jwtClaimsSet.issuer == null || signedJwt.jwtClaimsSet.issuer == clientId) {
            "issuer must be equal to the clientId"
        }

        check(signedJwt.jwtClaimsSet.audience == listOf(audience)) { "Invalid audience" }

        val issueTime = signedJwt.jwtClaimsSet.issueTime.time
        val now = currentTimeMillis()
        check(
            issueTime - proofIssueTimeToleranceMilliseconds <= now &&
                issueTime + proofIssueTimeToleranceMilliseconds >= now
        ) {
            "Invalid issue time"
        }

        try {
            nonceService.validate(signedJwt.jwtClaimsSet.claims["nonce"].toString(), C_NONCE)
        } catch (e: Exception) {
            throw IllegalStateException("Invalid nonce", e)
        }

        val jwk = signedJwt.header.jwk as? ECKey ?: error("jwk type not matching algorithm")
        check(signedJwt.verify(ECDSAVerifier(jwk))) { "Signature verification failed" }

        return signedJwt.header.jwk
    }
}
