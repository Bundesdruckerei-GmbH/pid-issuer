/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import tests.TestData.loadTestdata
import tests.encodeAndDecodeFromString

class CredentialResponseTest {

    @Test
    fun encodeAndDecodeMinimalCredentialResponse() {
        val response = CredentialResponse(credentials = listOf(Credential("credential")))

        val deserialized = Json.encodeAndDecodeFromString(response)

        assertThat(deserialized).isEqualTo(response)
    }

    @Test
    fun encodeAndDecodeCredentialResponseWithCredentials() {
        val response =
            CredentialResponse(
                credentials = listOf(Credential("credential01"), Credential("credential02"))
            )

        val serialized = Json.encodeToString(response)
        val deserialized = Json.decodeFromString<CredentialResponse>(serialized)

        assertThat(deserialized).isEqualTo(response)
    }

    @Test
    fun encodeAndDecodeCredentialResponseWithTransactionId() {
        val response = CredentialResponse(transactionId = "transaction-id")

        val deserialized = Json.encodeAndDecodeFromString(response)

        assertThat(deserialized).isEqualTo(response)
    }

    @Test
    fun encodeAndDecodeCompleteCredentialResponse() {
        val response =
            CredentialResponse(
                credentials = listOf(Credential("credential")),
                notificationId = "notification-id",
            )

        val deserialized = Json.encodeAndDecodeFromString(response)

        assertThat(deserialized).isEqualTo(response)
    }

    @Test
    fun `parse credential response containing credential `() {

        val credentialResponseRaw = loadTestdata("vci/credentialResponse1.json")

        val json: JsonElement = Json.decodeFromString(credentialResponseRaw)
        val credentialResponse: CredentialResponse = Json.decodeFromJsonElement(json)

        assertEquals(json, Json.encodeToJsonElement(credentialResponse))
    }

    @Test
    fun `parse credential response containing transaction id `() {

        val credentialResponseRaw = loadTestdata("vci/credentialResponse2.json")

        val json: JsonElement = Json.decodeFromString(credentialResponseRaw)
        val credentialResponse: CredentialResponse = Json.decodeFromJsonElement(json)

        assertEquals(json, Json.encodeToJsonElement(credentialResponse))
    }
}
