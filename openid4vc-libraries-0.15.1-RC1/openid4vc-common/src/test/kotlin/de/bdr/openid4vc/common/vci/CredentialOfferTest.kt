/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import tests.TestData.loadTestdata
import tests.encodeAndDecodeFromString

class CredentialOfferTest {

    @Test
    fun `parse sdjwt pre-authorized code credential offer `() {

        val credentialOfferRaw = loadTestdata("vci/credentialOffer_pre_sdjwt.json")

        val json: JsonElement = Json.decodeFromString(credentialOfferRaw)
        val credentialOffer: CredentialOffer = Json.decodeFromJsonElement(json)

        println(credentialOffer)

        assertEquals(json, Json.encodeToJsonElement(credentialOffer))
    }

    @Test
    fun `serialize and deserialize complete CredentialOffer`() {
        val offer =
            CredentialOffer(
                credentialIssuer = "http://example.com",
                credentialConfigurationIds = listOf("configuration-id"),
                grants =
                    Grants(
                        preAuthorizedCodeGrant =
                            PreAuthorizedCodeGrant(
                                preAuthorizedCode = "pre-auth-code",
                                txCodeMetadata =
                                    TxCodeMetadata(
                                        inputMode = InputMode.NUMERIC,
                                        length = 8,
                                        description = "tx code description"
                                    ),
                                authorizationServer = "http://a1.example.com",
                                interval = 494
                            ),
                        authorizationCodeGrant =
                            AuthorizationCodeGrant(
                                issuerState = "issuer-state",
                                authorizationServer = "http://a2.example.com"
                            )
                    )
            )

        val deserialized = Json.encodeAndDecodeFromString(offer)

        assertThat(deserialized).isEqualTo(offer)
    }

    @Test
    fun `deserialize tx code metadata with default values`() {
        val deserialized = Json.decodeFromString<TxCodeMetadata>("{}".trimIndent())

        assertThat(deserialized.inputMode).isEqualTo(InputMode.NUMERIC)
        assertThat(deserialized.length).isNull()
        assertThat(deserialized.description).isNull()
    }

    @Test
    fun `serialize and deserialize minimal CredentialOffer`() {
        val offer =
            CredentialOffer(
                credentialIssuer = "http://example.com",
                credentialConfigurationIds = listOf("configuration-id"),
                grants =
                    Grants(
                        preAuthorizedCodeGrant =
                            PreAuthorizedCodeGrant(preAuthorizedCode = "pre-auth-code"),
                        authorizationCodeGrant = AuthorizationCodeGrant()
                    )
            )

        val deserialized = Json.encodeAndDecodeFromString(offer)

        assertThat(deserialized).isEqualTo(offer)
    }

    @Test
    fun `serialize and deserialize minimal CredentialOffer with only auth code grant`() {
        val offer =
            CredentialOffer(
                credentialIssuer = "http://example.com",
                credentialConfigurationIds = listOf("configuration-id"),
                grants = Grants(authorizationCodeGrant = AuthorizationCodeGrant())
            )

        val deserialized = Json.encodeAndDecodeFromString(offer)

        assertThat(deserialized).isEqualTo(offer)
    }

    @Test
    fun `serialize and deserialize CredentialOffer with only pre auth code grant`() {
        val offer =
            CredentialOffer(
                credentialIssuer = "http://example.com",
                credentialConfigurationIds = listOf("configuration-id"),
                grants =
                    Grants(
                        preAuthorizedCodeGrant =
                            PreAuthorizedCodeGrant(preAuthorizedCode = "pre-auth-code")
                    )
            )

        val deserialized = Json.encodeAndDecodeFromString(offer)

        assertThat(deserialized).isEqualTo(offer)
    }

    @Test
    fun `serialize and deserialize CredentialOffer without grant`() {
        val offer =
            CredentialOffer(
                credentialIssuer = "http://example.com",
                credentialConfigurationIds = listOf("configuration-id")
            )

        val deserialized = Json.encodeAndDecodeFromString(offer)

        assertThat(deserialized).isEqualTo(offer)
    }
}
