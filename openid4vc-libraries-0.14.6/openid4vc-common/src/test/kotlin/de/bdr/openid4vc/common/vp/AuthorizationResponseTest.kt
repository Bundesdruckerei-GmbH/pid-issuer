/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vp

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.*
import tests.TestData.loadTestdata
import tests.encodeAndDecodeFromString

class AuthorizationResponseTest {

    @Test
    fun `decode Authorization Response`() {
        val authResponseExample = loadTestdata("vp/authorizationResponse_1.json")

        val json = Json.decodeFromString<JsonElement>(authResponseExample)

        val authResponse: AuthorizationResponse = Json.decodeFromJsonElement(json)

        // compare original with reserialized object
        assertEquals(json, Json.encodeToJsonElement<AuthorizationResponse>(authResponse))
    }

    @Test
    fun `encode and decode complete AuthorizationResponse`() {
        val authorizationResponse =
            AuthorizationResponse(
                vpToken = "vp-token",
                presentationSubmission =
                    PresentationSubmission(
                        id = "id",
                        definitionId = "definition-id",
                        descriptorMap =
                            listOf(
                                DescriptorMapElement(
                                    id = "descriptor-map-element-01",
                                    format = "sd-jwt-vc",
                                    path = "path"
                                ),
                                DescriptorMapElement(
                                    id = "descriptor-map-element-02",
                                    format = "sd-jwt-vc",
                                    path = "path2",
                                    pathNested =
                                        DescriptorMapElement(
                                            id = "descriptor-map-element-03",
                                            format = "sd-jwt-vc",
                                            path = "path3"
                                        )
                                )
                            )
                    ),
                state = "state",
                idToken = "id-token",
                iss = "https://example.com"
            )

        val deserialized = Json.encodeAndDecodeFromString(authorizationResponse)

        assertThat(deserialized).isEqualTo(authorizationResponse)
    }

    @Test
    fun `encode and decode minimal AuthorizationResponse`() {
        val authorizationResponse =
            AuthorizationResponse(
                vpToken = "vp-token",
                presentationSubmission =
                    PresentationSubmission(
                        id = "id",
                        definitionId = "definition-id",
                        descriptorMap =
                            listOf(
                                DescriptorMapElement(
                                    id = "descriptor-map-element-01",
                                    format = "sd-jwt-vc",
                                    path = "path"
                                )
                            )
                    )
            )

        val deserialized = Json.encodeAndDecodeFromString(authorizationResponse)

        assertThat(deserialized).isEqualTo(authorizationResponse)
    }
}
