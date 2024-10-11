/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException.ReasonCode.INVALID_CREDENTIAL_FORMAT
import de.bdr.openid4vc.common.formats.CredentialFormatRegistry
import de.bdr.openid4vc.common.vci.proofs.Proof
import de.bdr.openid4vc.common.vci.proofs.ProofsSerializer
import kotlin.reflect.full.createType
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable(with = CredentialRequestSerializer::class)
sealed class CredentialRequest {
    @SerialName("format") abstract val format: CredentialFormat?

    @SerialName("proof") abstract val proof: Proof?

    @SerialName("proofs")
    @Serializable(with = ProofsSerializer::class)
    abstract val proofs: List<Proof>

    @SerialName("credential_identifier") abstract val credentialIdentifier: String?

    @SerialName("credential_response_encryption")
    abstract val credentialEncryption: CredentialEncryption?

    protected fun validate() {
        require(proof == null || proofs.isEmpty()) { "Only proof OR proofs can be set, not both" }
    }
}

@Serializable
data class CredentialIdentifierBasedCredentialRequest(
    @SerialName("proof") override val proof: Proof? = null,
    @SerialName("proofs")
    @Serializable(with = ProofsSerializer::class)
    override val proofs: List<Proof> = emptyList(),
    @SerialName("credential_identifier") override val credentialIdentifier: String?,
    @SerialName("credential_response_encryption")
    override val credentialEncryption: CredentialEncryption? = null
) : CredentialRequest() {
    @SerialName("format") override val format: Nothing? = null

    init {
        validate()
    }
}

abstract class FormatSpecificCredentialRequest : CredentialRequest() {
    @SerialName("format") abstract override val format: CredentialFormat
    @SerialName("credential_identifier") override val credentialIdentifier: Nothing? = null
}

class CredentialRequestSerializer :
    JsonContentPolymorphicSerializer<CredentialRequest>(CredentialRequest::class) {

    @SuppressWarnings("UNCHECKED_CAST")
    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<CredentialRequest> {
        val format = element.jsonObject["format"]
        return if (format == null) {
            CredentialIdentifierBasedCredentialRequest.serializer()
        } else {
            val formatString =
                (format as? JsonPrimitive)?.content
                    ?: throw SerializationException("format of CredentialRequest must be a string")
            val requestClass =
                (CredentialFormatRegistry.registry[formatString]?.credentialRequestClass
                    ?: throw SpecificIllegalArgumentException(
                        INVALID_CREDENTIAL_FORMAT,
                        "CredentialRequest format $format unknown"
                    ))
            return serializer(requestClass.createType())
                as DeserializationStrategy<CredentialRequest>
        }
    }
}

@Serializable
data class CredentialEncryption(
    @SerialName("jwk") val jwk: JsonObject,
    @SerialName("alg") val alg: String,
    @SerialName("enc") val enc: String
)
