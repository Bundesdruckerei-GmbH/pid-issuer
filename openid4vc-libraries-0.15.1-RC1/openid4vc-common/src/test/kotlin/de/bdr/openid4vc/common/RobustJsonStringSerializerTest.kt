/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.json.Json

class RobustJsonStringSerializerTest {
    @Test
    fun serialize() {
        val value = Json.encodeToString(RobustJsonStringSerializer, "value")

        assertThat(value).isEqualTo(""""value"""")
    }

    @Test
    fun deserializeNumber() {
        val result = Json.decodeFromString(RobustJsonStringSerializer, """43.7""")

        assertThat(result).isEqualTo("43.7")
    }

    @Test
    fun deserializeString() {
        val result = Json.decodeFromString(RobustJsonStringSerializer, """"value"""")

        assertThat(result).isEqualTo("value")
    }
}
