/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.formats.sdjwtvc

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.vci.CredentialDescription
import de.bdr.openid4vc.common.vci.CredentialEncryption
import de.bdr.openid4vc.common.vci.FormatSpecificCredentialRequest
import de.bdr.openid4vc.common.vci.ProofTypeSupported
import de.bdr.openid4vc.common.vci.proofs.Proof
import de.bdr.openid4vc.common.vci.proofs.ProofType
import de.bdr.openid4vc.common.vci.proofs.ProofsSerializer
import de.bdr.openid4vc.common.vp.FormatDescription
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject

object SdJwtVcCredentialFormat : CredentialFormat {
    override val format = "vc+sd-jwt"
    override val credentialRequestClass = SdJwtVcCredentialRequest::class
    override val credentialDescriptionClass = SdJwtVcCredentialDescription::class
    override val formatDescriptionClass = SdJwtVcFormatDescription::class
}

@Serializable
data class SdJwtVcCredentialRequest(
    @SerialName("format")
    @EncodeDefault
    override val format: CredentialFormat = SdJwtVcCredentialFormat,
    @SerialName("proof") override val proof: Proof? = null,
    @SerialName("proofs")
    @Serializable(with = ProofsSerializer::class)
    override val proofs: List<Proof> = emptyList(),
    @SerialName("credential_response_encryption")
    override val credentialEncryption: CredentialEncryption? = null,
    @SerialName("vct") val vct: String,
) : FormatSpecificCredentialRequest() {
    init {
        require(format == SdJwtVcCredentialFormat) {
            "Format must be ${SdJwtVcCredentialFormat.format}"
        }
    }
}

/**
 * Defined in HAIP 7.2.2.
 * https://vcstuff.github.io/oid4vc-haip-sd-jwt-vc/draft-oid4vc-haip-sd-jwt-vc.html#section-7.2.2
 */
@Serializable
data class SdJwtVcCredentialDescription(
    @SerialName("format")
    @EncodeDefault
    override val format: CredentialFormat = SdJwtVcCredentialFormat,
    @SerialName("scope") override val scope: String? = null,
    @SerialName("cryptographic_binding_methods_supported")
    override val cryptographicBindingMethodsSupported: List<String>? = null,
    @SerialName("cryptographic_signing_alg_values_supported")
    override val cryptographicSigningAlgValuesSupported: List<String>? = null,
    @SerialName("proof_types_supported")
    override val proofTypesSupported: Map<ProofType, ProofTypeSupported>? = null,
    @SerialName("display") override val display: List<JsonObject>? = null,
    @SerialName("vct") val vct: String,
    /** Use [Claim] for the base interpretation of the values. */
    @SerialName("claims") val claims: JsonObject? = null,
    @SerialName("order") val order: List<String>? = null
) : CredentialDescription() {

    init {
        require(format == SdJwtVcCredentialFormat) {
            "Format must be ${SdJwtVcCredentialFormat.format}"
        }
    }

    /** Use [Display] for [DisplayType] for the base interpretation. */
    @Serializable
    data class Claim<DisplayType>(
        @SerialName("mandatory") val mandatory: Boolean = false,
        @SerialName("value_type") val valueType: String? = null,
        @SerialName("display") val display: List<DisplayType>? = null
    ) {
        @Serializable
        data class Display(
            @SerialName("name") val name: String? = null,
            @SerialName("locale") val locale: String? = null
        )
    }
}

@Serializable
class SdJwtVcFormatDescription : FormatDescription {
    @Transient override val type: CredentialFormat = SdJwtVcCredentialFormat

    override fun hashCode(): Int {
        return 394736936
    }

    override fun equals(other: Any?) = other is SdJwtVcFormatDescription
}
