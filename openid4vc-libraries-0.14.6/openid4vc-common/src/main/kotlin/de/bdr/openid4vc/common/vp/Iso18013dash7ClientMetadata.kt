/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Iso18013dash7ClientMetadata(
    @SerialName("jwks") val jwks: JsonElement,
    @SerialName("authorization_encrypted_response_alg") val responseAlg: String,
    @SerialName("authorization_encrypted_response_enc") val responseEnc: String,
    @SerialName("require_signed_request_object") val requireSignedRequestObject: Boolean,
    @SerialName("vp_formats") val vpFormats: JsonElement? = null,
    @SerialName("contacts") val contacts: List<String>? = null,
    @SerialName("logo_uri") val logoUri: String? = null,
    @SerialName("client_uri") val clientUri: String? = null,
    @SerialName("policy_uri") val policyUri: String? = null,
    @SerialName("tos_uri") val tosUri: String? = null
)
