/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import tests.encodeAndDecodeFromString

class TokenResponseTest {

    @Test
    fun serializeAndDeserializeMinimalTokenResponse() {
        val tokenResponse = TokenResponse(accessToken = "token", tokenType = "bearer")

        val deserialized = Json.encodeAndDecodeFromString(tokenResponse)

        assertThat(deserialized).isEqualTo(tokenResponse)
    }

    @Test
    fun serializeAndDeserializeCompleteTokenResponse() {
        val tokenResponse =
            TokenResponse(
                accessToken = "token",
                tokenType = "bearer",
                expiresIn = 984,
                credentialNonce = "nonce",
                credentialNonceExpiresIn = 588,
                refreshToken = "refresh-token",
                scope = "scope",
                state = "state"
            )

        val deserialized = Json.encodeAndDecodeFromString(tokenResponse)

        assertThat(deserialized).isEqualTo(tokenResponse)
    }
}
