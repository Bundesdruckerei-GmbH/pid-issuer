/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
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
    fun serializeAndDeserializeCompleteError() {
        val error = CredentialRequestError(error = "error01", errorDescription = "desc01")

        val deserialized = Json.encodeAndDecodeFromString(error)

        assertThat(deserialized).isEqualTo(error)
    }

    @Test
    fun deserializeError() {
        val expected = CredentialRequestError(error = "error01", errorDescription = "Foo")

        val deserialized =
            Json.decodeFromString<CredentialRequestError>(
                loadTestdata("vci/credentialRequestError.json")
            )

        assertThat(deserialized).isEqualTo(expected)
    }
}
