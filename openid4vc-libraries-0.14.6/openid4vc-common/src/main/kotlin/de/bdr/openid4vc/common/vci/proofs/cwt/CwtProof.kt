/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci.proofs.cwt

import COSE.OneKey
import com.upokecenter.cbor.CBORObject
import de.bdr.openid4vc.common.Algorithm
import de.bdr.openid4vc.common.currentTimeMillis
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException
import de.bdr.openid4vc.common.signing.Signer
import de.bdr.openid4vc.common.vci.proofs.InvalidNonceException
import de.bdr.openid4vc.common.vci.proofs.Proof
import de.bdr.openid4vc.common.vci.proofs.ProofType
import de.bdr.openid4vc.common.vci.proofs.StringProofsValueSerializer
import java.util.*
import kotlinx.serialization.*
import kotlinx.serialization.Transient

object CwtProofType : ProofType {
    override val value = "cwt"
    override val proofClass = CwtProof::class
    override val proofsValueSerializer =
        StringProofsValueSerializer(toString = CwtProof::cwt, fromString = ::CwtProof)
}

@Serializable
data class CwtProof
@OptIn(ExperimentalSerializationApi::class)
constructor(
    val cwt: String,
    @SerialName("proof_type") @EncodeDefault override val proofType: ProofType = CwtProofType,
) : Proof {

    init {
        require(proofType == CwtProofType) { "Proof type must be CwtProofType" }
    }

    companion object {

        val CWT_PROOF_CTY = "openid4vci-proof+cwt"

        fun create(
            clientId: String?,
            audience: String,
            nonce: String,
            signer: Signer,
            issueTimestamp: Long = currentTimeMillis(),
            useCwtTag: Boolean = false
        ): CwtProof {
            val cwt = Cwt()
            cwt.alg = signer.algorithm.coseAlgorithm.AsCBOR()
            cwt.cty = CWT_PROOF_CTY
            cwt.iss = clientId
            cwt.aud = audience
            cwt.nonce = nonce
            cwt.iat = Date(issueTimestamp)
            cwt.coseKey = OneKey(signer.keys.jwk.toECKey().toECPublicKey(), null)
            return CwtProof(
                Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(cwt.toBytes(signer, useCwtTag))
            )
        }
    }

    @Transient
    val decodedCwt =
        try {
            Cwt.fromBytes(Base64.getUrlDecoder().decode(cwt))
        } catch (e: Throwable) {
            throw SpecificIllegalArgumentException(
                SpecificIllegalArgumentException.ReasonCode.INVALID_PROOF,
                "Failed to parse CWT",
                e
            )
        }

    fun validate(
        clientId: String?,
        audience: String,
        nonce: String,
        proofIssueTimeToleranceMillis: Long = 30_000,
    ): OneKey {

        val key = decodedCwt.coseKey ?: error("Missing COSE_Key")

        check(Algorithm.entries.any { it.coseAlgorithm.AsCBOR() == decodedCwt.alg }) {
            "Unsupported algorithm ${decodedCwt.alg}"
        }

        check(decodedCwt.cty == CWT_PROOF_CTY) { "cty is not openid4vci-proof+cwt" }

        check(decodedCwt.protectedHeader[CBORObject.FromObject(33)] == null) {
            "x5chain must not be set" // library does not support that right now
        }

        check(decodedCwt.validate(key)) { "Signature is invalid" }

        check(decodedCwt.iss == null || decodedCwt.iss == clientId) { "iss not equal to client id" }

        check(decodedCwt.aud == audience) { "invalid aud" }

        val issueTime = decodedCwt.iat?.time ?: error("Missing iat")
        val now = currentTimeMillis()
        check(
            issueTime - proofIssueTimeToleranceMillis <= now &&
                issueTime + proofIssueTimeToleranceMillis >= now
        ) {
            "Invalid iat"
        }

        if (decodedCwt.nonce != nonce) throw InvalidNonceException()

        return key
    }
}
