/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.base.requests

import com.nimbusds.jwt.SignedJWT
import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException.ReasonCode.INVALID_PROOF
import de.bdr.openid4vc.common.vci.CredentialDescription
import de.bdr.openid4vc.common.vci.CredentialEncryption
import de.bdr.openid4vc.common.vci.FormatSpecificCredentialRequest
import de.bdr.openid4vc.common.vci.ProofTypeSupported
import de.bdr.openid4vc.common.vci.proofs.Proof
import de.bdr.openid4vc.common.vci.proofs.ProofType
import de.bdr.openid4vc.common.vci.proofs.ProofsSerializer
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProof
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProofType
import de.bdr.openid4vc.common.vp.FormatDescription
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject

object SeedCredentialFormat : CredentialFormat {
    override val format = "seed_credential"
    override val credentialRequestClass = SeedCredentialRequest::class
    override val credentialDescriptionClass = SeedCredentialDescription::class
    override val formatDescriptionClass = SeedFormatDescription::class

    init {
        register()
    }
}

@Serializable
data class SeedCredentialRequest @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("format")
    @EncodeDefault
    override val format: CredentialFormat = SeedCredentialFormat,
    @SerialName("proof")
    override val proof: Proof? = null,
    @SerialName("proofs")
    @Serializable(with = ProofsSerializer::class)
    override val proofs: List<Proof> = emptyList(),
    @SerialName("credential_response_encryption")
    override val credentialEncryption: CredentialEncryption? = null,
    @SerialName("pin_derived_eph_key_pop")
    private val _pinDerivedEphKeyPop: String
) : FormatSpecificCredentialRequest() {
    init {
        require(format == SeedCredentialFormat) {
            "Format must be ${SeedCredentialFormat.format}"
        }
        require(proofs.isEmpty() || proofs.size == 1) {
            "Multiple proofs not supported"
        }
        require(proof != null || proofs.size == 1) {
            "No proof provided"
        }
        val oneProof = proof ?: proofs.first()
        if (oneProof.proofType !is JwtProofType) {
            throw SpecificIllegalArgumentException(INVALID_PROOF, "Proof is not jwt")
        }
    }

    @Transient
    val pinDerivedEphKeyPop: SignedJWT =
        try {
            SignedJWT.parse(_pinDerivedEphKeyPop)
        } catch (e: Exception) {
            throw SpecificIllegalArgumentException(INVALID_PROOF, "Failed to parse jwt", e)
        }

    val singleProof: JwtProof = (proof ?: proofs.first()) as JwtProof
}

data class SeedCredentialDescription(
    override val format: CredentialFormat = SeedCredentialFormat,
    override val scope: String? = null,
    override val cryptographicBindingMethodsSupported: List<String>? = null,
    override val cryptographicSigningAlgValuesSupported: List<String>? = null,
    override val proofTypesSupported: Map<ProofType, ProofTypeSupported>? = null,
    override val display: List<JsonObject>? = null,
) : CredentialDescription() {
    init {
        throw NotImplementedError()
    }
}

class SeedFormatDescription : FormatDescription {
    @Transient
    override val type: CredentialFormat = SeedCredentialFormat

    init {
        throw NotImplementedError()
    }
}