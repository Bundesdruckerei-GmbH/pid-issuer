/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.common.hexToByteArray
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class KeyAuthorizationsTest {
    private val keyAuthorizationsByteString = "A26A6E616D65537061636573826A6E616D655370616365316A6E616D655370616365326C64617461456C656D656E7473A26A6E616D65537061636531826A676976656E5F6E616D656B66616D696C795F6E616D656A6E616D65537061636532826B6973737565725F646174656B6578706972795F64617465"
    private lateinit var keyAuthorizationsCBOR: CBORObject

    private val keyAuthorizationsWithOnlyNameSpacesByteString = "A16A6E616D65537061636573826A6E616D655370616365316A6E616D65537061636532"
    private lateinit var keyAuthorizationsWithOnlyNameSpacesByteStringCBOR: CBORObject

    private val keyAuthorizationsWithOnlyDataElementsByteString = "A16C64617461456C656D656E7473A26A6E616D65537061636531826A676976656E5F6E616D656B66616D696C795F6E616D656A6E616D65537061636532826B6973737565725F646174656B6578706972795F64617465"
    private lateinit var keyAuthorizationsWithOnlyDataElementsByteStringCBOR: CBORObject

    private lateinit var authorizedNameSpaces: AuthorizedNameSpaces
    private lateinit var authorizedDataElements: AuthorizedDataElements

    private lateinit var keyAuthorizations: KeyAuthorizations

    @Rule
    @JvmField
    var thrown: ExpectedException = ExpectedException.none()

    @Before
    fun setUp() {
        keyAuthorizationsCBOR = CBORObject.DecodeFromBytes(keyAuthorizationsByteString.hexToByteArray())
        keyAuthorizationsWithOnlyNameSpacesByteStringCBOR = CBORObject.DecodeFromBytes(keyAuthorizationsWithOnlyNameSpacesByteString.hexToByteArray())
        keyAuthorizationsWithOnlyDataElementsByteStringCBOR = CBORObject.DecodeFromBytes(keyAuthorizationsWithOnlyDataElementsByteString.hexToByteArray())


        authorizedNameSpaces = arrayListOf("nameSpace1", "nameSpace2")
        authorizedDataElements = mutableMapOf(
                Pair("nameSpace1", arrayListOf("given_name", "family_name")),
                Pair("nameSpace2", arrayListOf("issuer_date", "expiry_date"))
        )

        keyAuthorizations = KeyAuthorizations(authorizedNameSpaces, authorizedDataElements)
    }

    @Test
    fun asCBOR() {
        assertEquals(keyAuthorizationsCBOR, keyAuthorizations.asCBOR())
    }

    @Test
    fun fromCBOR() {
        val keyAuthorizationsFromCBOR = KeyAuthorizations.fromCBOR(keyAuthorizationsCBOR)

        assertEquals(keyAuthorizations.authorizedDataElements, keyAuthorizationsFromCBOR.authorizedDataElements)
        assertEquals(keyAuthorizations.authorizedNameSpaces, keyAuthorizationsFromCBOR.authorizedNameSpaces)
    }

    @Test
    fun shouldThrowExceptionWhenKeyAuthorizationsIsEmpty() {
        thrown.expect(IllegalArgumentException::class.java)

        KeyAuthorizations()
    }

    @Test
    fun asCBORWithOnlyNameSpaces() {
        val keyAuthorizationsWithOneArgument = KeyAuthorizations(authorizedNameSpaces)

        assertEquals(keyAuthorizationsWithOnlyNameSpacesByteStringCBOR, keyAuthorizationsWithOneArgument.asCBOR())
    }

    @Test
    fun fromCBORWithOnlyNameSpaces() {
        val keyAuthorizationsWithOneArgument = KeyAuthorizations(authorizedNameSpaces)

        val keyAuthorizationsWithOneArgumentFromCBOR = KeyAuthorizations.fromCBOR(keyAuthorizationsWithOnlyNameSpacesByteStringCBOR)

        assertEquals(keyAuthorizationsWithOneArgument.authorizedDataElements, keyAuthorizationsWithOneArgumentFromCBOR.authorizedDataElements)
        assertEquals(keyAuthorizationsWithOneArgument.authorizedNameSpaces, keyAuthorizationsWithOneArgumentFromCBOR.authorizedNameSpaces)
    }

    @Test
    fun asCBORWithOnlyDataElements() {
        val keyAuthorizationsWithOneArgument = KeyAuthorizations(authorizedDataElements = authorizedDataElements)

        assertEquals(keyAuthorizationsWithOnlyDataElementsByteStringCBOR, keyAuthorizationsWithOneArgument.asCBOR())
    }

    @Test
    fun fromCBORWithOnlyDataElements() {
        val keyAuthorizationsWithOneArgument = KeyAuthorizations(authorizedDataElements = authorizedDataElements)

        val keyAuthorizationsWithOneArgumentFromCBOR = KeyAuthorizations.fromCBOR(keyAuthorizationsWithOnlyDataElementsByteStringCBOR)

        assertEquals(keyAuthorizationsWithOneArgument.authorizedNameSpaces, keyAuthorizationsWithOneArgumentFromCBOR.authorizedNameSpaces)
        assertEquals(keyAuthorizationsWithOneArgument.authorizedDataElements, keyAuthorizationsWithOneArgumentFromCBOR.authorizedDataElements)
    }
}
