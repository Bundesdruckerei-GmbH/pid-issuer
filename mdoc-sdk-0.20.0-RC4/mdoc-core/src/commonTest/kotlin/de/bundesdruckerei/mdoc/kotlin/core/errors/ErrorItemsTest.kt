/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.errors

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.common.hexToByteArray
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class ErrorItemsTest {
    private val errorItemByteString = "A26F646F63756D656E745F6E756D626572036A676976656E5F6E616D6505"
    private lateinit var errorItemCBOR: CBORObject
    private lateinit var errorItems: ErrorItems

    @Rule
    @JvmField
    var thrown: ExpectedException = ExpectedException.none()

    @Before
    fun setUp() {
        errorItemCBOR = CBORObject.DecodeFromBytes(errorItemByteString.hexToByteArray())

        ErrorItemsTest()

        errorItems = ErrorItems(mutableMapOf(Pair("document_number", 3), Pair("given_name", 5)))
    }

    @Test
    fun fromCBOR() {
        assertEquals(
            errorItems.errors["document_number"],
            ErrorItems.fromCBOR(errorItemCBOR).errors["document_number"]
        )
    }

    @Test
    fun asCBOR() {
        assertEquals(errorItemCBOR, errorItems.asCBOR())
    }
}
