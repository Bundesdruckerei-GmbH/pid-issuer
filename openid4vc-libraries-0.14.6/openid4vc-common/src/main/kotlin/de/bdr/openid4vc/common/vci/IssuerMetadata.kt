/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.RobustJsonStringSerializer
import de.bdr.openid4vc.common.UnsupportedCredentialDescription
import de.bdr.openid4vc.common.formats.CredentialFormatRegistry
import de.bdr.openid4vc.common.vci.CredentialDescription.Display.Logo
import de.bdr.openid4vc.common.vci.proofs.ProofType
import kotlin.reflect.full.createType
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class IssuerMetadata(
    @SerialName("credential_issuer") val credentialIssuer: String,
    @SerialName("authorization_servers") val authorizationServers: List<String>? = null,
    @SerialName("credential_endpoint") val credentialEndpoint: String,
    @SerialName("batch_credential_endpoint") val batchCredentialEndpoint: String? = null,
    @SerialName("deferred_credential_endpoint") val deferredCredentialEndpoint: String? = null,
    @SerialName("credential_response_encryption")
    val credentialResponseEncryption: CredentialResponseEncryption? = null,
    @SerialName("credential_identifiers_supported")
    val credentialIdentifiersSupported: Boolean? = null,
    /** Use [Display] for the base interpretation. */
    @SerialName("display") val display: List<JsonObject>? = null,
    @SerialName("signed_metadata") val signedMetadata: String? = null,
    @SerialName("credential_configurations_supported")
    val credentialConfigurationsSupported: Map<String, CredentialDescription>,
) {
    init {
        if (signedMetadata != null) {
            "Signed metadata is not supported, but was received during deserialization"
        }
    }

    /** Use [Logo] for [LogoType] for the base interpretation. */
    @Serializable
    data class Display<LogoType>(
        @SerialName("name") val name: String? = null,
        @SerialName("locale") val locale: String? = null,
        @SerialName("logo") val logo: LogoType? = null
    ) {

        @Serializable
        data class Logo(
            @SerialName("uri") val uri: String,
            @SerialName("alt_text") val altText: String? = null
        )
    }
}

@Serializable
data class CredentialResponseEncryption(
    /**
     * Array containing a list of the JWE RFC7516 encryption algorithms (alg values) RFC7518
     * supported by the Credential and Batch Credential Endpoint to encode the Credential or Batch
     * Credential Response in a JWT RFC7519.
     */
    @SerialName("alg_values_supported") val algValuesSupported: List<String>,
    /**
     * Array containing a list of the JWE RFC7516 encryption algorithms (enc values) RFC7518
     * supported by the Credential and Batch Credential Endpoint to encode the Credential or Batch
     * Credential Response in a JWT RFC7519.
     */
    @SerialName("enc_values_supported") val encValuesSupported: List<String>,
    /**
     * Boolean value specifying whether the Credential Issuer requires the additional encryption on
     * top of TLS for the Credential Response. If the value is true, the Credential Issuer requires
     * encryption for every Credential Response and therefore the Wallet MUST provide encryption
     * keys in the Credential Request. If the value is false, the Wallet MAY chose whether it
     * provides encryption keys or not.
     */
    @SerialName("encryption_required") val encryptionRequired: Boolean
)

@Serializable(with = CredentialDescriptionSerializer::class)
abstract class CredentialDescription {
    @SerialName("format") abstract val format: CredentialFormat
    @SerialName("proof_types_supported")
    abstract val proofTypesSupported: Map<ProofType, ProofTypeSupported>?
    @SerialName("scope") abstract val scope: String?
    @SerialName("cryptographic_binding_methods_supported")
    abstract val cryptographicBindingMethodsSupported: List<String>?
    @SerialName("cryptographic_signing_alg_values_supported")
    abstract val cryptographicSigningAlgValuesSupported: List<String>?
    /** Use [Display] for base interpretation of values. */
    @SerialName("display") abstract val display: List<JsonObject>?

    /**
     * Use [Logo] for [LogoType] and [BackgroundImage] for [BackgroundImageType] for the base
     * interpretation.
     */
    @Serializable
    data class Display<LogoType, BackgroundImageType>(
        @SerialName("name") val name: String,
        @SerialName("locale") val locale: String? = null,
        @SerialName("logo") val logo: LogoType? = null,
        @SerialName("description") val description: String? = null,
        @SerialName("background_color") val backgroundColor: String? = null,
        @SerialName("background_image") val backgroundImage: BackgroundImageType? = null,
        @SerialName("text_color") val textColor: String? = null
    ) {

        @Serializable
        data class Logo(
            @SerialName("uri") val uri: String,
            @SerialName("alt_text") val altText: String? = null
        )

        @Serializable
        data class BackgroundImage(
            @SerialName("uri") val uri: String,
            @SerialName("text_color") val textColor: String? = null
        )
    }
}

class CredentialDescriptionSerializer :
    JsonContentPolymorphicSerializer<CredentialDescription>(CredentialDescription::class) {

    @SuppressWarnings("UNCHECKED_CAST")
    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<out CredentialDescription> {
        val format = element.jsonObject["format"]?.jsonPrimitive?.content
        val credentialDescriptionClass =
            CredentialFormatRegistry.registry[format]?.credentialDescriptionClass
                ?: UnsupportedCredentialDescription::class

        return serializer(credentialDescriptionClass.createType())
            as DeserializationStrategy<out CredentialDescription>
    }
}

@Serializable
data class ProofTypeSupported(
    @SerialName("proof_signing_alg_values_supported")
    val signingAlgValuesSupported: List<@Serializable(RobustJsonStringSerializer::class) String>,
    // TODO These are only there for the interop event and will probably be changed afterwards.
    @SerialName("proof_alg_values_supported") val cwtAlgValuesSupported: List<Int>? = null,
    @SerialName("proof_crv_values_supported") val cwtCrvValuesSupported: List<Int>? = null
)
