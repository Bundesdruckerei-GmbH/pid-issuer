/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common

import de.bdr.openid4vc.common.formats.CredentialFormatRegistry
import de.bdr.openid4vc.common.vci.CredentialDescription
import de.bdr.openid4vc.common.vci.CredentialRequest
import de.bdr.openid4vc.common.vci.ProofTypeSupported
import de.bdr.openid4vc.common.vci.proofs.ProofType
import de.bdr.openid4vc.common.vp.FormatDescription
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

@Serializable(with = CredentialFormatSerializer::class)
interface CredentialFormat {
    val format: String

    /** Used as credential request class */
    val credentialRequestClass: KClass<out CredentialRequest>

    /** Used in the issuer metadata to describe the credential */
    val credentialDescriptionClass: KClass<out CredentialDescription>

    /** Used to describe the requested credential in the presentation definition */
    val formatDescriptionClass: KClass<out FormatDescription>

    fun register() {
        CredentialFormatRegistry.registry[format] = this
    }
}

@Serializable
class UnsupportedCredentialDescription(
    @SerialName("format") override val format: CredentialFormat,
    @SerialName("scope") override val scope: String? = null,
    @SerialName("cryptographic_binding_methods_supported")
    override val cryptographicBindingMethodsSupported: List<String>? = null,
    @SerialName("cryptographic_signing_alg_values_supported")
    override val cryptographicSigningAlgValuesSupported: List<String>? = null,
    @SerialName("proof_types_supported")
    override val proofTypesSupported: Map<ProofType, ProofTypeSupported>? = null,
    @SerialName("display") override val display: List<JsonObject>? = null
) : CredentialDescription()

class UnsupportedCredentialFormat(override val format: String) : CredentialFormat {
    override val credentialRequestClass: KClass<out CredentialRequest>
        get() = throw NotImplementedError("No CredentialRequest for unsupported credential format")

    override val credentialDescriptionClass: KClass<out CredentialDescription>
        get() =
            throw NotImplementedError("No CredentialDescription for unsupported credential format")

    override val formatDescriptionClass: KClass<out FormatDescription>
        get() = throw NotImplementedError("No FormatDescription for unsupported credential format")

    override fun register() {
        throw NotImplementedError("This class ist not for registration")
    }
}

class CredentialFormatSerializer : KSerializer<CredentialFormat> {

    private val delegate = serializer<String>()

    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): CredentialFormat {
        val format = decoder.decodeSerializableValue(delegate)
        return CredentialFormatRegistry.registry[format]
            ?: if (decoder is JsonDecoder && decoder.json.configuration.ignoreUnknownKeys) {
                UnsupportedCredentialFormat(format)
            } else throw IllegalArgumentException("CredentialFormat $format not registered")
    }

    override fun serialize(encoder: Encoder, value: CredentialFormat) {
        encoder.encodeSerializableValue(delegate, value.format)
    }
}
