/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tests.encodeAndDecodeFromString

class ParResponseTest {

    @Test
    fun serializeAndDeserializeCompleteParResponse() {
        val parResponse =
            ParResponse(requestUri = "https://example.com/request-01", expiresIn = 48949)

        val deserialized = Json.encodeAndDecodeFromString(parResponse)

        assertThat(deserialized).isEqualTo(parResponse)
    }
}
