/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vp

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.nimbusds.jwt.SignedJWT
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcFormatDescription
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
                presentationDefinitionUri = "https://example.com/presentation-definiton"
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
                                                                        )
                                                                )
                                                            ),
                                                        optional = false,
                                                        intentToRetain = true
                                                    ),
                                                    Field(path = listOf("path-b"))
                                                ),
                                            limitDisclosure = "required"
                                        )
                                )
                            )
                    )
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
                                                                        )
                                                                )
                                                            ),
                                                        optional = false,
                                                        intentToRetain = true
                                                    ),
                                                    Field(path = listOf("path-b"))
                                                ),
                                            limitDisclosure = "required"
                                        )
                                )
                            )
                    )
            )

        val deserialized =
            Json.decodeFromString<AuthorizationRequest>(
                loadTestdata("vp/authorizationRequestComplete.json")
            )

        assertThat(deserialized).isEqualTo(expected)
    }
}
