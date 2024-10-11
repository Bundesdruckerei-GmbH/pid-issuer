/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AuthorizationRequest(
    /* REQUIRED: Defined in RFC6749 */
    @SerialName("response_type") val responseType: String,
    /* REQUIRED: Defined in RFC6749 */
    @SerialName("client_id") val clientId: String,
    /* RECOMMENDED: Defined in RFC6749 */
    @SerialName("state") val state: String? = null,
    /* OPTIONAL: Defined in RFC6749 */
    @SerialName("scope") val scope: String? = null,
    /* OPTIONAL: Defined in RFC6749 */
    @SerialName("redirect_uri") val redirectUri: String? = null,
    @SerialName("response_uri") val responseUri: String? = null,
    /* OPTIONAL: Defined in OI4VP */
    @SerialName("client_metadata") val clientMetadata: JsonObject? = null,
    /* OPTIONAL: Defined in OI4VP */
    @SerialName("client_metadata_uri") val clientMetadataUri: String? = null,
    /* OPTIONAL: Defined in OI4VP */
    @SerialName("client_id_scheme") val clientIdScheme: String? = null,
    /* RECOMMENDED: Defined in OI4VP */
    @SerialName("nonce") val nonce: String,
    /* OPTIONAL: Defined in OAuth.Responses */
    @SerialName("response_mode") val responseMode: String? = null,
    @SerialName("presentation_definition")
    val presentationDefinition: PresentationDefinition? = null,
    @SerialName("presentation_definition_uri") val presentationDefinitionUri: String? = null,
    @SerialName("aud") val audience: String? = null,
)
