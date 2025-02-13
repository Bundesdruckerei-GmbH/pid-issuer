/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import de.bdr.openid4vc.common.mapStructureToJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.*
import tests.TestData.loadTestdata
import tests.encodeAndDecodeFromString

class Iso18013dash7ClientMetadataTest {

    @org.junit.jupiter.api.Test
    fun serializeAndDeserializeMinimalMetadata() {
        val response =
            Iso18013dash7ClientMetadata(
                jwks =
                    JsonObject(
                        mapOf(
                            "foo" to
                                ECKeyGenerator(Curve.P_256)
                                    .generate()
                                    .toPublicJWK()
                                    .toJSONObject()
                                    .mapStructureToJson()
                                    .jsonObject
                        )
                    ),
                responseAlg = "alg",
                responseEnc = "enc",
                requireSignedRequestObject = false
            )

        val deserialized = Json.encodeAndDecodeFromString(response)

        assertThat(deserialized).isEqualTo(response)
    }

    @org.junit.jupiter.api.Test
    fun serializeAndDeserializeCompleteMetadata() {
        val response =
            Iso18013dash7ClientMetadata(
                jwks =
                    JsonObject(
                        mapOf(
                            "foo" to
                                ECKeyGenerator(Curve.P_256)
                                    .generate()
                                    .toPublicJWK()
                                    .toJSONObject()
                                    .mapStructureToJson()
                                    .jsonObject
                        )
                    ),
                responseAlg = "alg",
                responseEnc = "enc",
                requireSignedRequestObject = false,
                vpFormats = JsonPrimitive("vp-formats"),
                contacts = listOf("contact01", "contact02"),
                logoUri = "https://example.com/logo-uri",
                clientUri = "https://example.com/client-uri",
                policyUri = "https://example.com/policy-uri",
                tosUri = "https://example.com/tos-uri"
            )

        val deserialized = Json.encodeAndDecodeFromString(response)

        assertThat(deserialized).isEqualTo(response)
    }

    @Test
    fun `decode client meta data according to ISO18013-7`() {
        val metadataExample = loadTestdata("vp/iso18013dash7ClientMetadata.json")

        val json = Json.decodeFromString<JsonElement>(metadataExample)

        val clientMetadata: Iso18013dash7ClientMetadata = Json.decodeFromJsonElement(json)

        assertEquals("ECDH-ES", clientMetadata.responseAlg)
        assertEquals("A256GCM", clientMetadata.responseEnc)
        assertEquals(false, clientMetadata.requireSignedRequestObject)
        assertEquals(
            JsonObject(mapOf("mso_mdoc" to JsonObject(emptyMap()))),
            clientMetadata.vpFormats
        )

        assertEquals(
            JsonObject(
                mapOf(
                    "keys" to
                        JsonArray(
                            listOf(
                                JsonObject(
                                    mapOf(
                                        "kty" to JsonPrimitive("EC"),
                                        "crv" to JsonPrimitive("P-256"),
                                        "x" to
                                            JsonPrimitive(
                                                "psIq6slE7F1yZ3ara0hsfSlAz5qinuf5jtk4dHbRorY"
                                            ),
                                        "y" to
                                            JsonPrimitive(
                                                "ptKGAp2Mh8RwecGzEHSJbGuW0osoOAHmSZz_rVjeddM"
                                            )
                                    )
                                )
                            )
                        )
                )
            ),
            clientMetadata.jwks
        )

        // compare original with reserialized object
        assertEquals(json, Json.encodeToJsonElement<Iso18013dash7ClientMetadata>(clientMetadata))
    }

    @Test
    fun `ignore empty properties`() {
        val metadataExample = loadTestdata("vp/iso18013dash7ClientMetadataWOContacts.json")

        val json = Json.decodeFromString<JsonElement>(metadataExample)

        val clientMetadata: Iso18013dash7ClientMetadata = Json.decodeFromJsonElement(json)
        // compare original with re-serialized object
        assertEquals(json, Json.encodeToJsonElement<Iso18013dash7ClientMetadata>(clientMetadata))
    }
}
