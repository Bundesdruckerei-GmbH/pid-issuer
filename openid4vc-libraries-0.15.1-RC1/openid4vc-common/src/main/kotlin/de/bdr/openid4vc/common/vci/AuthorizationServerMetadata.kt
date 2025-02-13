/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthorizationServerMetadata(
    /* REQUIRED: defined RFC8414 */
    @SerialName("issuer") val issuer: String,
    /* OPTIONAL, required unless no grant types are supported that use the authorization endpoint.: defined RFC8414 */
    @SerialName("authorization_endpoint") val authorizationEndpoint: String? = null,
    /* REQUIRED (unless implicit grant type is supported): defined RFC8414 */
    @SerialName("token_endpoint") val tokenEndpoint: String,
    /* REQUIRED: defined RFC8414 */
    /* OPTIONAL: by OID4VCI */
    @SerialName("response_types_supported") val responseTypesSupported: List<String>? = null,
    /* OPTIONAL: defined RFC8414 */
    @SerialName("token_endpoint_auth_methods_supported")
    val tokenEndpointAuthMethods: List<String>? = listOf("client_secret_basic"),
    /* Required for PAR: definied in OAuth 2.0 Pushed Authorization Requests */
    @SerialName("pushed_authorization_request_endpoint")
    val pushedAuthorizationRequestEndpoint: String? = null,
    @SerialName("require_pushed_authorization_requests")
    val requirePushedAuthorizationRequests: Boolean = false,
    @SerialName("code_challenge_methods_supported")
    val codeChallengeMethodsSupported: List<String>? = null,
    /* defined in RFC 9449 */
    @SerialName("dpop_signing_alg_values_supported")
    val dpopSigningAlgValuesSupported: List<String>? = null,
    /* OPTIONAL: defined RFC8414 */
    @SerialName("grant_types_supported") val grantTypesSupported: List<String>? = null
)
