/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
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
                refreshToken = "refresh-token",
                scope = "scope",
                state = "state",
            )

        val deserialized = Json.encodeAndDecodeFromString(tokenResponse)

        assertThat(deserialized).isEqualTo(tokenResponse)
    }
}
