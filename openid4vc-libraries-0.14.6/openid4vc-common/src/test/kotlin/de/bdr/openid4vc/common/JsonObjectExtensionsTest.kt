/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.lang.IllegalArgumentException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JsonObjectExtensionsTest {

    @Test
    fun testMapStructureToJson() {
        val result =
            mapOf(
                "foo" to "foo",
                "bar" to 5,
                "baz" to true,
                "foobar" to listOf("foo", 5, true),
                "abc" to mapOf("bar" to 5, "baz" to "foo")
            )

        assertThat(result.mapStructureToJson())
            .isEqualTo(
                JsonObject(
                    mapOf(
                        "foo" to JsonPrimitive("foo"),
                        "bar" to JsonPrimitive(5),
                        "baz" to JsonPrimitive(true),
                        "foobar" to
                            JsonArray(
                                listOf(JsonPrimitive("foo"), JsonPrimitive(5), JsonPrimitive(true))
                            ),
                        "abc" to
                            JsonObject(
                                mapOf("bar" to JsonPrimitive(5), "baz" to JsonPrimitive("foo"))
                            )
                    )
                )
            )
    }

    @Test
    fun testMapStructureToJsonWithUnsupportedType() {
        assertThrows<IllegalArgumentException> { Unit.mapStructureToJson() }
    }

    @Test
    fun testJsonToMapStructureWithNonConvertiblePrimitive() {
        assertThrows<IllegalArgumentException> {
            JsonPrimitive(
                    object : Number() {
                        override fun toString() = "invalid"

                        override fun toByte() = 0.toByte()

                        override fun toDouble() = 0.toDouble()

                        override fun toFloat() = 0.toFloat()

                        override fun toInt() = 0

                        override fun toLong() = 0.toLong()

                        override fun toShort() = 0.toShort()
                    }
                )
                .jsonToMapStructure()
        }
    }

    @Test
    fun testJsonToMapStructure() {
        val map =
            JsonObject(
                    mapOf(
                        "null" to JsonNull,
                        "foo" to JsonPrimitive("foo"),
                        "int" to JsonPrimitive(5),
                        "long" to JsonPrimitive(Int.MAX_VALUE.toLong() + 1),
                        "biginteger" to JsonPrimitive(Long.MAX_VALUE.toBigInteger().inc()),
                        "float" to JsonPrimitive(Float.MAX_VALUE),
                        "double" to JsonPrimitive(Double.MAX_VALUE),
                        "bigdecimal" to
                            JsonPrimitive(
                                Double.MAX_VALUE.toBigDecimal().multiply(100000.0.toBigDecimal())
                            ),
                        "true" to JsonPrimitive(true),
                        "false" to JsonPrimitive(false),
                        "foobar" to
                            JsonArray(
                                listOf(JsonPrimitive("foo"), JsonPrimitive(5), JsonPrimitive(true))
                            ),
                        "abc" to
                            JsonObject(
                                mapOf("bar" to JsonPrimitive(5), "baz" to JsonPrimitive("foo"))
                            )
                    )
                )
                .jsonToMapStructure()

        assertThat(map)
            .isEqualTo(
                mapOf(
                    "null" to null,
                    "foo" to "foo",
                    "int" to 5,
                    "long" to Int.MAX_VALUE.toLong() + 1,
                    "biginteger" to Long.MAX_VALUE.toBigInteger().inc(),
                    "float" to Float.MAX_VALUE,
                    "double" to Double.MAX_VALUE,
                    "bigdecimal" to
                        Double.MAX_VALUE.toBigDecimal().multiply(100000.0.toBigDecimal()),
                    "true" to true,
                    "false" to false,
                    "foobar" to listOf("foo", 5, true),
                    "abc" to mapOf("bar" to 5, "baz" to "foo")
                )
            )
    }
}
