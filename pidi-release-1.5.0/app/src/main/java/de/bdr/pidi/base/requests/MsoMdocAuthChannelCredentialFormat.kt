/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.base.requests

import com.nimbusds.jose.jwk.JWK
import de.bdr.openid4vc.common.Base64UrlByteArraySerializer
import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.JwkSerializer
import de.bdr.openid4vc.common.vci.CredentialDescription
import de.bdr.openid4vc.common.vci.CredentialEncryption
import de.bdr.openid4vc.common.vci.FormatSpecificCredentialRequest
import de.bdr.openid4vc.common.vci.ProofTypeSupported
import de.bdr.openid4vc.common.vci.proofs.Proof
import de.bdr.openid4vc.common.vci.proofs.ProofType
import de.bdr.openid4vc.common.vci.proofs.ProofsSerializer
import de.bdr.openid4vc.common.vp.FormatDescription
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialDescription.Claim
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialDescription.Claim.Display
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject

object MsoMdocAuthChannelCredentialFormat : CredentialFormat {
    override val format = "mso_mdoc_authenticated_channel"
    override val credentialRequestClass = MsoMdocAuthChannelCredentialRequest::class
    override val credentialDescriptionClass = MsoMdocAuthChannelCredentialDescription::class
    override val formatDescriptionClass = MsoMdocAuthChannelFormatDescription::class

    var registered = false
        private set

    init {
        register()
        registered = true
    }
}

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
annotation class Generated

@Serializable
data class MsoMdocAuthChannelCredentialRequest @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("format")
    @EncodeDefault
    override val format: CredentialFormat = MsoMdocAuthChannelCredentialFormat,
    @SerialName("proof") override val proof: Proof? = null,
    @SerialName("proofs")
    @Serializable(with = ProofsSerializer::class)
    override val proofs: List<Proof> = emptyList(),
    @SerialName("credential_response_encryption")
    override val credentialEncryption: CredentialEncryption? = null,
    @SerialName("doctype") val doctype: String,
    @Serializable(with = JwkSerializer::class)
    @SerialName("verifier_pub")
    val verifierPub: JWK,
    @Serializable(with = Base64UrlByteArraySerializer::class)
    @SerialName("session_transcript")
    val sessionTranscript: ByteArray
) : FormatSpecificCredentialRequest() {
    init {
        validate()
        require (format == MsoMdocAuthChannelCredentialFormat) {
            "Format must be ${MsoMdocAuthChannelCredentialFormat.format}"
        }
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MsoMdocAuthChannelCredentialRequest

        if (format != other.format) return false
        if (proof != other.proof) return false
        if (proofs != other.proofs) return false
        if (credentialEncryption != other.credentialEncryption) return false
        if (doctype != other.doctype) return false
        if (verifierPub != other.verifierPub) return false
        if (!sessionTranscript.contentEquals(other.sessionTranscript)) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + (proof?.hashCode() ?: 0)
        result = 31 * result + proofs.hashCode()
        result = 31 * result + (credentialEncryption?.hashCode() ?: 0)
        result = 31 * result + doctype.hashCode()
        result = 31 * result + verifierPub.hashCode()
        result = 31 * result + sessionTranscript.contentHashCode()
        return result
    }
}

/* Definied in OID4VCI Appendix E ISO mDL */
@Serializable
data class MsoMdocAuthChannelCredentialDescription @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("format")
    @EncodeDefault
    override val format: CredentialFormat = MsoMdocAuthChannelCredentialFormat,
    @SerialName("scope") override val scope: String? = null,
    @SerialName("cryptographic_binding_methods_supported")
    override val cryptographicBindingMethodsSupported: List<String>? = null,
    @SerialName("cryptographic_signing_alg_values_supported")
    override val cryptographicSigningAlgValuesSupported: List<String>? = null,
    @SerialName("credential_alg_values_supported")
    val credentialAlgValuesSupported: List<Int>? = null,
    @SerialName("credential_crv_values_supported")
    val credentialCrvValuesSupported: List<Int>? = null,
    @SerialName("policy") val policy: Policy? = null,
    @SerialName("proof_types_supported")
    override val proofTypesSupported: Map<ProofType, ProofTypeSupported>? = null,
    @SerialName("display") override val display: List<JsonObject>? = null,
    /** Use [Claim] for the base interpretation of the values. */
    @SerialName("claims") val claims: Map<String, JsonObject>? = null,
    @SerialName("doctype") val doctype: String,
) : CredentialDescription() {

    init {
        require(format == MsoMdocAuthChannelCredentialFormat) {
            "Format must be ${MsoMdocAuthChannelCredentialFormat.format}"
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
data class MsoMdocAuthChannelFormatDescription(
    @SerialName("alg") val alg: List<String>,
) : FormatDescription {
    @Transient override val type: CredentialFormat = MsoMdocAuthChannelCredentialFormat
}
