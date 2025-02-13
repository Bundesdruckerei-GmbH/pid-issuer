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
