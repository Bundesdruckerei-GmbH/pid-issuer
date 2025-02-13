/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.base.requests

import com.nimbusds.jose.jwk.JWK
import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.JwkSerializer
import de.bdr.openid4vc.common.vci.CredentialDescription
import de.bdr.openid4vc.common.vci.CredentialEncryption
import de.bdr.openid4vc.common.vci.FormatSpecificCredentialRequest
import de.bdr.openid4vc.common.vci.proofs.Proof
import de.bdr.openid4vc.common.vci.proofs.ProofType
import de.bdr.openid4vc.common.vci.proofs.ProofTypeConfiguration
import de.bdr.openid4vc.common.vci.proofs.ProofsSerializer
import de.bdr.openid4vc.common.vp.CredentialQueryMatcher
import de.bdr.openid4vc.common.vp.dcql.CredentialQuery
import de.bdr.openid4vc.common.vp.dcql.VerificationSettings
import de.bdr.openid4vc.common.vp.pex.FormatDescription
import de.bdr.openid4vc.common.vp.pex.InputDescriptor
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialDescription.Claim
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialDescription.Claim.Display
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.KClass

private const val NOT_IMPLEMENTED_REASON = "currently not used"

object SdJwtVcAuthChannelCredentialFormat : CredentialFormat {
    override val format = "vc+sd-jwt_authenticated_channel"
    override val credentialRequestClass = SdJwtVcAuthChannelCredentialRequest::class
    override val credentialDescriptionClass = SdJwtVcAuthChannelCredentialDescription::class
    override val credentialQueryClass: KClass<out CredentialQuery>
        get() = TODO(NOT_IMPLEMENTED_REASON)
    override val credentialQueryMatcher: CredentialQueryMatcher<CredentialQuery, VerificationSettings?>
        get() = TODO(NOT_IMPLEMENTED_REASON)
    override val formatDescriptionClass = SdJwtVcAuthChannelFormatDescription::class
    override val inputDescriptorMatcher: CredentialQueryMatcher<InputDescriptor, Boolean>
        get() = TODO(NOT_IMPLEMENTED_REASON)

    var registered = false
        private set

    init {
        register()
        registered = true
    }
}

@Serializable
data class SdJwtVcAuthChannelCredentialRequest @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("format")
    @EncodeDefault
    override val format: CredentialFormat = SdJwtVcAuthChannelCredentialFormat,
    @SerialName("proof") override val proof: Proof? = null,
    @SerialName("proofs")
    @Serializable(with = ProofsSerializer::class)
    override val proofs: List<Proof> = emptyList(),
    @SerialName("credential_response_encryption")
    override val credentialEncryption: CredentialEncryption? = null,
    @SerialName("vct") val vct: String,
    @Serializable(with = JwkSerializer::class)
    @SerialName("verifier_pub")
    val verifierPub: JWK
) : FormatSpecificCredentialRequest() {
    init {
        validate()
        require (format == SdJwtVcAuthChannelCredentialFormat) {
            "Format must be ${SdJwtVcAuthChannelCredentialFormat.format}"
        }
    }
}

/**
 * Defined in HAIP 7.2.2.
 * https://vcstuff.github.io/oid4vc-haip-sd-jwt-vc/draft-oid4vc-haip-sd-jwt-vc.html#section-7.2.2
 */
@Serializable
data class SdJwtVcAuthChannelCredentialDescription @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("format")
    @EncodeDefault
    override val format: CredentialFormat = SdJwtVcAuthChannelCredentialFormat,
    @SerialName("scope") override val scope: String? = null,
    @SerialName("cryptographic_binding_methods_supported")
    override val cryptographicBindingMethodsSupported: List<String>? = null,
    @SerialName("cryptographic_signing_alg_values_supported")
    override val cryptographicSigningAlgValuesSupported: List<String>? = null,
    @SerialName("proof_types_supported")
    override val proofTypesSupported: Map<ProofType, ProofTypeConfiguration>? = null,
    @SerialName("display") override val display: List<JsonObject>? = null,
    @SerialName("vct") val vct: String,
    /** Use [Claim] for the base interpretation of the values. */
    @SerialName("claims") val claims: JsonObject? = null,
    @SerialName("order") val order: List<String>? = null
) : CredentialDescription() {

    init {
        require(format == SdJwtVcAuthChannelCredentialFormat) {
            "Format must be ${SdJwtVcAuthChannelCredentialFormat.format}"
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
class SdJwtVcAuthChannelFormatDescription : FormatDescription {
    @Transient override val type: CredentialFormat = SdJwtVcAuthChannelCredentialFormat

    override fun hashCode(): Int {
        return 394736936
    }

    override fun equals(other: Any?) = other is SdJwtVcAuthChannelFormatDescription
}
