/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import com.upokecenter.numbers.EInteger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.Period
import java.util.*

private const val s = "signed"

class ValidityInfoTest {

    private val now = Instant.now()
    private val signed = Date.from(now)
    private val validFrom = signed
    private val validUntil = Date.from(now.plus(Period.ofDays(14)))
    private val expectedUpdate = Date.from(now.plus(Period.ofDays(12)))

    private val validityInfo1 = ValidityInfo(signed, validFrom, validUntil, expectedUpdate)

    private val cborObject = CBORObject.NewMap()
        .Set("signed", signed)
        .Set("validFrom", validFrom)
        .Set("validUntil", validUntil)
        .Set("expectedUpdate", expectedUpdate)

    private val validityInfo2 = ValidityInfo(cborObject)


    @Test
    fun asCBOR() {
        assertEquals(cborObject, validityInfo1.asCBOR())
        assertEquals(cborObject, validityInfo2.asCBOR())
    }

    @Test
    fun getSigned() {
        assertEquals(signed, validityInfo1.signed)
        assertEquals(signed, validityInfo2.signed)
    }

    @Test
    fun getValidFrom() {
        assertEquals(validFrom, validityInfo1.validFrom)
        assertEquals(validFrom, validityInfo2.validFrom)
    }

    @Test
    fun getValidUntil() {
        assertEquals(validUntil, validityInfo1.validUntil)
        assertEquals(validUntil, validityInfo2.validUntil)
    }

    @Test
    fun getExpectedUpdate() {
        assertEquals(expectedUpdate, validityInfo1.expectedUpdate)
        assertEquals(expectedUpdate, validityInfo2.expectedUpdate)
    }

    @Test
    fun shouldUseTdate() {
        val signedObj = cborObject.get("signed")
        assertTrue(signedObj.isTagged)
        assertTrue(signedObj.mostInnerTag.isZero)

        val validFromObj = cborObject.get("validFrom")
        assertTrue(validFromObj.isTagged)
        assertTrue(validFromObj.mostInnerTag.isZero)

        val validUntilObj = cborObject.get("validUntil")
        assertTrue(validUntilObj.isTagged)
        assertTrue(validUntilObj.mostInnerTag.isZero)

        val expectedUpdateObj = cborObject.get("expectedUpdate")
        assertTrue(expectedUpdateObj.isTagged)
        assertTrue(expectedUpdateObj.mostInnerTag.isZero)
    }
}
