/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth


import de.bundesdruckerei.mdoc.kotlin.core.common.InvalidContextTagException
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException


class ContextTagTest {

    @Rule
    @JvmField
    var thrown: ExpectedException = ExpectedException.none()

    @Test
    fun getTag() {
        assertEquals("deviceMac", ContextTag.Mac.tag)
        assertEquals("deviceSignature", ContextTag.Sig.tag)
    }

    @Test
    fun fromString() {
        assertEquals(ContextTag.Mac, ContextTag.fromString("deviceMac"))
        assertEquals(ContextTag.Sig, ContextTag.fromString("deviceSignature"))
    }

    //This method always returns first element of enumset
    @Test
    fun shouldThrowInvalidContextTagWhenTagIsInvalid() {
        thrown.expect(InvalidContextTagException::class.java)

        assertEquals(ContextTag.Mac, ContextTag.fromString("Test"))
        assertEquals(ContextTag.Mac, ContextTag.fromString("BetterTest"))
        assertEquals(ContextTag.Mac, ContextTag.fromString("TheBestTest"))
    }
}