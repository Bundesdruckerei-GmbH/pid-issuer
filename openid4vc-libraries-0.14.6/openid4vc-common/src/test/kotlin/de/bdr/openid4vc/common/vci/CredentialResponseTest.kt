/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.lang.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.assertThrows
import tests.TestData.loadTestdata
import tests.encodeAndDecodeFromString

class CredentialResponseTest {

    @Test
    fun constructCredentialResponseWithCredentialAndCredentialsSet() {
        assertThrows<IllegalArgumentException> {
            CredentialResponse(
                credential = "credential",
                credentials = listOf("credential-01", "credential-02")
            )
        }
    }

    @Test
    fun encodeAndDecodeMinimalCredentialResponse() {
        val response = CredentialResponse(credential = "credential")

        val deserialized = Json.encodeAndDecodeFromString(response)

        assertThat(deserialized).isEqualTo(response)
    }

    @Test
    fun encodeAndDecodeCredentialResponseWithCredentials() {
        val response = CredentialResponse(credentials = listOf("credential01", "credential02"))

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
                credential = "credential",
                credentialNonce = "nonce",
                credentialNonceExpiresIn = 4894,
                notificationId = "notification-id"
            )

        val deserialized = Json.encodeAndDecodeFromString(response)

        assertThat(deserialized).isEqualTo(response)
    }

    @Test
    fun encodeAndDecodeBatchResponse() {
        val batchResponse =
            BatchCredentialResponse(
                listOf(
                    Json.decodeFromString(loadTestdata("vci/credentialResponse1.json")),
                    Json.decodeFromString(loadTestdata("vci/credentialResponse2.json"))
                )
            )

        val deserialized = Json.encodeAndDecodeFromString(batchResponse)

        assertThat(deserialized).isEqualTo(batchResponse)
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
