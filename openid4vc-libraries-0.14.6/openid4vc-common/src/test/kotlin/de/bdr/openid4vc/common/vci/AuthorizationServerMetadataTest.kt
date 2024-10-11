/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import tests.TestData.loadTestdata
import tests.encodeAndDecodeFromString

class AuthorizationServerMetadataTest {

    @Test
    fun serializeAndDeserializeMinimalData() {
        val data =
            AuthorizationServerMetadata(
                issuer = "https://example.com",
                tokenEndpoint = "https://example.com/token"
            )

        val deserialized = Json.encodeAndDecodeFromString(data)

        assertThat(deserialized).isEqualTo(data)
    }

    @Test
    fun serializeAndDeserializeCompleteData() {
        val data =
            AuthorizationServerMetadata(
                issuer = "https://example.com",
                tokenEndpoint = "https://example.com/token",
                authorizationEndpoint = "https://example.com/authorization",
                responseTypesSupported = listOf("response-type-a"),
                tokenEndpointAuthMethods = listOf("client_secret_basic"),
                pushedAuthorizationRequestEndpoint = "https://example.com/par",
                requirePushedAuthorizationRequests = true,
                codeChallengeMethodsSupported = listOf("S256"),
                dpopSigningAlgValuesSupported = listOf("dpop-alg-a"),
                grantTypesSupported = listOf("grant-type-a")
            )

        val deserialized = Json.encodeAndDecodeFromString(data)

        assertThat(deserialized).isEqualTo(data)
    }

    @Test
    fun deserializeCompleteData() {
        val expected =
            AuthorizationServerMetadata(
                issuer = "https://example.com",
                tokenEndpoint = "https://example.com/token",
                authorizationEndpoint = "https://example.com/authorization",
                responseTypesSupported = listOf("response-type-a"),
                tokenEndpointAuthMethods = listOf("client_secret_basic"),
                pushedAuthorizationRequestEndpoint = "https://example.com/par",
                requirePushedAuthorizationRequests = true,
                codeChallengeMethodsSupported = listOf("S256"),
                dpopSigningAlgValuesSupported = listOf("dpop-alg-a"),
                grantTypesSupported = listOf("grant-type-a")
            )

        val deserialized =
            Json.decodeFromString<AuthorizationServerMetadata>(
                loadTestdata("vci/authServerMetadata.json")
            )

        assertThat(deserialized).isEqualTo(expected)
    }
}
