/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.lang.IllegalArgumentException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tests.TestData.loadTestdata
import tests.encodeAndDecodeFromString

class CredentialRequestErrorTest {

    @Test
    fun serializeAndDeserializeMinimalError() {
        val error = CredentialRequestError(error = "error01")

        val deserialized = Json.encodeAndDecodeFromString(error)

        assertThat(deserialized).isEqualTo(error)
    }

    @Test
    fun serializeAndDeserializeErrorWithoutExpiresIn() {
        val error =
            CredentialRequestError(
                error = "error01",
                errorDescription = "desc01",
                credentialNonce = "nonce"
            )

        val deserialized = Json.encodeAndDecodeFromString(error)

        assertThat(deserialized).isEqualTo(error)
    }

    @Test
    fun serializeAndDeserializeCompleteError() {
        val error =
            CredentialRequestError(
                error = "error01",
                errorDescription = "desc01",
                credentialNonce = "nonce",
                credentialNonceExpiresIn = 3029
            )

        val deserialized = Json.encodeAndDecodeFromString(error)

        assertThat(deserialized).isEqualTo(error)
    }

    @Test
    fun constructErrorWithMissingNonce() {
        assertThrows<IllegalArgumentException> {
            CredentialRequestError(error = "error", credentialNonceExpiresIn = 400)
        }
    }

    @Test
    fun deserializeError() {
        val expected =
            CredentialRequestError(
                error = "error01",
                errorDescription = "Foo",
                credentialNonce = "nonce",
                credentialNonceExpiresIn = 894
            )

        val deserialized =
            Json.decodeFromString<CredentialRequestError>(
                loadTestdata("vci/credentialRequestError.json")
            )

        assertThat(deserialized).isEqualTo(expected)
    }

    @Test
    fun deserializeErrorWithMissingNonce() {
        assertThrows<IllegalArgumentException> {
            Json.decodeFromString<CredentialRequestError>(
                loadTestdata("vci/credentialRequestErrorWithoutNonce.json")
            )
        }
    }
}
