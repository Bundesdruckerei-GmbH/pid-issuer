/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tests.encodeAndDecodeFromString

class AuthorizationResponseResponseTest {

    @Test
    fun serializeAndDeserializeMinimalAuthorizationResponseResponse() {
        val response = AuthorizationResponseResponse()

        val deserialized = Json.encodeAndDecodeFromString(response)

        assertThat(deserialized).isEqualTo(response)
    }

    @Test
    fun serializeAndDeserializeCompleteAuthorizationResponseResponse() {
        val response = AuthorizationResponseResponse(redirectUri = "https://example.com/redirect")

        val deserialized = Json.encodeAndDecodeFromString(response)

        assertThat(deserialized).isEqualTo(response)
    }
}
