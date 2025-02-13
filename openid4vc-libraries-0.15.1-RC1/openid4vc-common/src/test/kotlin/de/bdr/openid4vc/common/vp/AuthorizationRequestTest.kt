/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.nimbusds.jwt.SignedJWT
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcFormatDescription
import de.bdr.openid4vc.common.vp.pex.Constraints
import de.bdr.openid4vc.common.vp.pex.Field
import de.bdr.openid4vc.common.vp.pex.InputDescriptor
import de.bdr.openid4vc.common.vp.pex.PresentationDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.*
import tests.TestData.loadTestdata
import tests.encodeAndDecodeFromString

class AuthorizationRequestTest {

    @Test
    fun `decode Authorization Request JWT`() {
        val jwtExample = loadTestdata("vp/presentation-definition/authorizationRequest.jwt")

        val jwt = SignedJWT.parse(jwtExample)

        val json = Json.decodeFromString<JsonElement>(jwt.payload.toString())

        val authRequest: AuthorizationRequest = Json.decodeFromJsonElement(json)

        // compare original with reserialized object
        assertEquals(json, Json.encodeToJsonElement<AuthorizationRequest>(authRequest))
    }

    @Test
    fun `encode and decode minimal authorization request`() {
        val request =
            AuthorizationRequest(
                responseType = "jwt",
                clientId = "http://example.com",
                nonce = "nonce",
                presentationDefinitionUri = "https://example.com/presentation-definiton",
            )

        val deserialized = Json.encodeAndDecodeFromString(request)

        assertThat(deserialized).isEqualTo(request)
    }

    @Test
    fun `encode and decode complete authorization request`() {
        val request =
            AuthorizationRequest(
                responseType = "jwt",
                clientId = "http://example.com",
                state = "ff00112233",
                scope = "test",
                redirectUri = "https://example.com/redirect",
                responseUri = "https://example.com/response",
                clientMetadata = JsonObject(mapOf("test" to JsonPrimitive("test"))),
                nonce = "nonce",
                audience = "audience",
                responseMode = "responseMode",
                presentationDefinition =
                    PresentationDefinition(
                        id = "ff00",
                        inputDescriptors =
                            listOf(
                                InputDescriptor(
                                    id = "foo",
                                    format = SdJwtVcFormatDescription(),
                                    constraints =
                                        Constraints(
                                            fields =
                                                listOf(
                                                    Field(
                                                        path = listOf("path-a"),
                                                        id = "field-id",
                                                        purpose = "field-purpose",
                                                        name = "field-name",
                                                        filter =
                                                            JsonObject(
                                                                mapOf(
                                                                    "type" to
                                                                        JsonPrimitive("string"),
                                                                    "const" to
                                                                        JsonPrimitive(
                                                                            "expected-value"
                                                                        ),
                                                                )
                                                            ),
                                                        optional = false,
                                                        intentToRetain = true,
                                                    ),
                                                    Field(path = listOf("path-b")),
                                                ),
                                            limitDisclosure = "required",
                                        ),
                                )
                            ),
                    ),
            )

        val deserialized = Json.encodeAndDecodeFromString(request)

        assertThat(deserialized).isEqualTo(request)
    }

    @Test
    fun `decode complete authorization request`() {
        val expected =
            AuthorizationRequest(
                responseType = "jwt",
                clientId = "http://example.com",
                state = "ff00112233",
                scope = "test",
                redirectUri = "https://example.com/redirect",
                responseUri = "https://example.com/response",
                clientMetadata = JsonObject(mapOf("test" to JsonPrimitive("test"))),
                nonce = "nonce",
                audience = "audience",
                responseMode = "responseMode",
                presentationDefinition =
                    PresentationDefinition(
                        id = "ff00",
                        inputDescriptors =
                            listOf(
                                InputDescriptor(
                                    id = "foo",
                                    format = SdJwtVcFormatDescription(),
                                    constraints =
                                        Constraints(
                                            fields =
                                                listOf(
                                                    Field(
                                                        path = listOf("path-a"),
                                                        id = "field-id",
                                                        purpose = "field-purpose",
                                                        name = "field-name",
                                                        filter =
                                                            JsonObject(
                                                                mapOf(
                                                                    "type" to
                                                                        JsonPrimitive("string"),
                                                                    "const" to
                                                                        JsonPrimitive(
                                                                            "expected-value"
                                                                        ),
                                                                )
                                                            ),
                                                        optional = false,
                                                        intentToRetain = true,
                                                    ),
                                                    Field(path = listOf("path-b")),
                                                ),
                                            limitDisclosure = "required",
                                        ),
                                )
                            ),
                    ),
            )

        val deserialized =
            Json.decodeFromString<AuthorizationRequest>(
                loadTestdata("vp/authorizationRequestComplete.json")
            )

        assertThat(deserialized).isEqualTo(expected)
    }

    @Test
    fun `decode authorization request from URI`() {

        val exampleUri =
            "https://client.example.org/universal-link?" +
                "response_type=vp_token" +
                "&client_id=redirect_uri:https%3A%2F%2Fclient.example.org%2Fcb" +
                "&redirect_uri=https%3A%2F%2Fclient.example.org%2Fcb" +
                "&presentation_definition_uri=https://uri.example.com/presentation-definition" +
                "&nonce=n-0S6_WzA2Mj" +
                "&client_metadata=%7B%22vp_formats%22:%7B%22jwt_vp_json%22:%" +
                "7B%22alg%22:%5B%22EdDSA%22,%22ES256K%22%5D%7D,%22ldp" +
                "_vp%22:%7B%22proof_type%22:%5B%22Ed25519Signature201" +
                "8%22%5D%7D%7D%7D"

        val request =
            AuthorizationRequestBaseClass.fromUriString(exampleUri) as AuthorizationRequest

        assertThat(request.clientId).isEqualTo("redirect_uri:https://client.example.org/cb")
    }
}
