/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    /**
     * RFC6749
     *
     * RECOMMENDED. The lifetime in seconds of the access token. For example, the value "3600"
     * denotes that the access token will expire in one hour from the time the response was
     * generated. If omitted, the authorization server SHOULD provide the expiration time via other
     * means or document the default value.
     */
    @SerialName("expires_in") val expiresIn: Int? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("scope") val scope: String? = null,
    /**
     * RFC6749
     *
     * REQUIRED if the "state" parameter was present in the client authorization request. The exact
     * value received from the client.
     */
    @SerialName("state") val state: String? = null,
)
