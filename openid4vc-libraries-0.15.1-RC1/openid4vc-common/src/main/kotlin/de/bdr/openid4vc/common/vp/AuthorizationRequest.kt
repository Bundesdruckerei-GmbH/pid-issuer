/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp

import de.bdr.openid4vc.common.vp.dcql.DcqlQuery
import de.bdr.openid4vc.common.vp.pex.PresentationDefinition
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.Charset
import kotlin.reflect.full.createType
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer

@Serializable(with = AuthorizationRequestBaseClassSerializer::class)
sealed class AuthorizationRequestBaseClass {
    @SerialName("client_id") abstract val clientId: String
    @SerialName("client_metadata") abstract val clientMetadata: JsonObject?

    companion object {
        fun fromUriString(
            uriString: String,
            json: Json = Json { ignoreUnknownKeys = true },
        ): AuthorizationRequestBaseClass {
            val uri = URI(uriString)
            require(uri.query?.isNotBlank() ?: false) { "no query parameters found" }

            val jsonObject =
                JsonObject(
                    uri.query.split("&").associate { param ->
                        val parameterParts = param.split("=", limit = 2)
                        val key = parameterParts[0]
                        val valueDecoded =
                            parameterParts.getOrNull(1)
                                ?: "".let { URLDecoder.decode(it, Charset.defaultCharset()).trim() }

                        val valueJson =
                            if (valueDecoded.startsWith("{") && valueDecoded.endsWith("}"))
                                json.decodeFromString<JsonObject>(valueDecoded)
                            else {
                                JsonPrimitive(valueDecoded)
                            }

                        key to valueJson
                    }
                )

            return json.decodeFromJsonElement(jsonObject)
        }
    }
}

class AuthorizationRequestBaseClassSerializer :
    JsonContentPolymorphicSerializer<AuthorizationRequestBaseClass>(
        AuthorizationRequestBaseClass::class
    ) {
    @OptIn(ExperimentalSerializationApi::class)
    @SuppressWarnings("UNCHECKED_CAST")
    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<AuthorizationRequestBaseClass> {
        val authorizationRequestClass =
            if (element.jsonObject.containsKey("request_uri"))
                AuthorizationRequestByReference::class
            else AuthorizationRequest::class

        return serializer(authorizationRequestClass.createType())
            as DeserializationStrategy<AuthorizationRequestBaseClass>
    }
}

@Serializable
data class AuthorizationRequest(
    /* REQUIRED: Defined in RFC6749 */
    @SerialName("response_type") val responseType: String,
    /* REQUIRED: Defined in RFC6749 */
    @SerialName("client_id") override val clientId: String,
    /* RECOMMENDED: Defined in RFC6749 */
    @SerialName("state") val state: String? = null,
    /* OPTIONAL: Defined in RFC6749 */
    @SerialName("scope") val scope: String? = null,
    /* OPTIONAL: Defined in RFC6749 */
    @SerialName("redirect_uri") val redirectUri: String? = null,
    @SerialName("response_uri") val responseUri: String? = null,
    /* OPTIONAL: Defined in OI4VP */
    @SerialName("client_metadata") override val clientMetadata: JsonObject? = null,
    /* OPTIONAL: Defined in OI4VP */
    @SerialName("client_metadata_uri") val clientMetadataUri: String? = null,
    /* OPTIONAL: Defined in OI4VP */
    @Deprecated("This field was removed in OpenID4VP-draft 22. Use prefix of the client_id instead")
    @SerialName("client_id_scheme")
    val clientIdScheme: String? = null,
    /* RECOMMENDED: Defined in OI4VP */
    @SerialName("nonce") val nonce: String,
    /* OPTIONAL: Defined in OAuth.Responses */
    @SerialName("response_mode") val responseMode: String? = null,
    @SerialName("presentation_definition")
    val presentationDefinition: PresentationDefinition? = null,
    @SerialName("vp_query") val vpQuery: DcqlQuery? = null,
    @SerialName("presentation_definition_uri") val presentationDefinitionUri: String? = null,
    @SerialName("aud") val audience: String? = null,
) : AuthorizationRequestBaseClass() {
    init {
        val numQueryParamsSet =
            listOf(presentationDefinition, vpQuery, presentationDefinitionUri).count { it != null }
        require(numQueryParamsSet == 1) {
            "Exactly one of presentation_definition, presentation_definition_uri or vp_query must be set"
        }
    }
}

@Serializable
data class AuthorizationRequestByReference(
    @SerialName("client_id") override val clientId: String,
    @SerialName("client_metadata") override val clientMetadata: JsonObject? = null,
    @SerialName("request_uri") val requestUri: String,
    @SerialName("request_uri_method") val requestUriMethod: RequestUriMethod? = null,
) : AuthorizationRequestBaseClass()

@Serializable
enum class RequestUriMethod {
    @SerialName("post") POST,
    @SerialName("get") GET,
}
