/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.formats.msomdoc

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialDescription.Claim
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialDescription.Claim.Display
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

object MsoMdocCredentialFormat : CredentialFormat {
    override val format = "mso_mdoc"
    override val credentialRequestClass = MsoMdocCredentialRequest::class
    override val credentialDescriptionClass = MsoMdocCredentialDescription::class
    override val formatDescriptionClass = MsoMdocFormatDescription::class
}

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
annotation class Generated

@Serializable
data class MsoMdocCredentialRequest(
    @SerialName("format")
    @EncodeDefault
    override val format: CredentialFormat = MsoMdocCredentialFormat,
    @SerialName("proof") override val proof: Proof? = null,
    @SerialName("proofs")
    @Serializable(with = ProofsSerializer::class)
    override val proofs: List<Proof> = emptyList(),
    @SerialName("credential_response_encryption")
    override val credentialEncryption: CredentialEncryption? = null,
    @SerialName("doctype") val doctype: String
    // END TODO
) : FormatSpecificCredentialRequest() {
    init {
        validate()
        require(format == MsoMdocCredentialFormat) {
            "Format must be ${MsoMdocCredentialFormat.format}"
        }
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MsoMdocCredentialRequest

        if (format != other.format) return false
        if (proof != other.proof) return false
        if (proofs != other.proofs) return false
        if (credentialEncryption != other.credentialEncryption) return false
        if (doctype != other.doctype) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + (proof?.hashCode() ?: 0)
        result = 31 * result + proofs.hashCode()
        result = 31 * result + (credentialEncryption?.hashCode() ?: 0)
        result = 31 * result + doctype.hashCode()
        return result
    }
}

/* Definied in OID4VCI Appendix E ISO mDL */
@Serializable
data class MsoMdocCredentialDescription(
    @SerialName("format")
    @EncodeDefault
    override val format: CredentialFormat = MsoMdocCredentialFormat,
    @SerialName("scope") override val scope: String? = null,
    @SerialName("cryptographic_binding_methods_supported")
    override val cryptographicBindingMethodsSupported: List<String>? = null,
    @SerialName("cryptographic_signing_alg_values_supported")
    override val cryptographicSigningAlgValuesSupported: List<String>? = null,
    // TODO These are only there for the interop event and will probably be changed afterwards.
    @SerialName("credential_alg_values_supported")
    val credentialAlgValuesSupported: List<Int>? = null,
    @SerialName("credential_crv_values_supported")
    val credentialCrvValuesSupported: List<Int>? = null,
    @SerialName("policy") val policy: Policy? = null,
    // END TODO
    @SerialName("proof_types_supported")
    override val proofTypesSupported: Map<ProofType, ProofTypeSupported>? = null,
    @SerialName("display") override val display: List<JsonObject>? = null,
    /** Use [Claim] for the base interpretation of the values. */
    @SerialName("claims") val claims: Map<String, JsonObject>? = null,
    @SerialName("doctype") val doctype: String,
) : CredentialDescription() {

    init {
        require(format == MsoMdocCredentialFormat) {
            "Format must be ${MsoMdocCredentialFormat.format}"
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
data class Policy(
    @SerialName("batch_size") val batchSize: Int,
    @SerialName("one_time_use") val oneTimeUse: Boolean
)

@Serializable
data class MsoMdocFormatDescription(
    @SerialName("alg") val alg: List<String>,
) : FormatDescription {
    @Transient override val type: CredentialFormat = MsoMdocCredentialFormat
}
