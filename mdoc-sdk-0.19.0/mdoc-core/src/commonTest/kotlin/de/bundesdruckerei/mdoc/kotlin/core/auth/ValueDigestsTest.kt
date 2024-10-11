/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.common.toByteArray
import de.bundesdruckerei.mdoc.kotlin.toHexString
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ValueDigestsTest{
    private val valueDigestByteString = "A2716F72672E69736F2E31383031332E352E31A3015820AF307AD59C28C5032C1A49394E0BCE7CB673A533D9AD12BD4CA980F6F12DB5BE02582076AAC7B52F938A4AEAE31D796BA5AEB2DBE0C9D96AD75B18D75AFAE875A79C76035820E2503B905A1754EF483BF5DD429C3A9BC66BDF7B3D1C080CFDEB4EC27AADEE6378186F72672E69736F2E31383031332E352E312E55746F706961A1183458208D5FFF80E966D52C123C34B07BD6546909AD7AE6BF48D736E0865A8B99853B7F"
    private val digestsByteString = "A3015820AF307AD59C28C5032C1A49394E0BCE7CB673A533D9AD12BD4CA980F6F12DB5BE02582076AAC7B52F938A4AEAE31D796BA5AEB2DBE0C9D96AD75B18D75AFAE875A79C76035820E2503B905A1754EF483BF5DD429C3A9BC66BDF7B3D1C080CFDEB4EC27AADEE63"

    private lateinit var valueDigestCBOR: CBORObject
    private lateinit var valueDigest: ValueDigests


    @Before
    fun setUp() {
        valueDigestCBOR = CBORObject.DecodeFromBytes(valueDigestByteString.toByteArray())
        ValueDigestsTest()
        valueDigest = ValueDigests.fromCBOR(valueDigestCBOR)
    }

    @Test
    fun fromCBOR() {
        assertEquals(valueDigest.digests, ValueDigests.fromCBOR(valueDigestCBOR).digests)
    }

    @Test
    fun getNameSpaces() {
        assertEquals("[org.iso.18013.5.1, org.iso.18013.5.1.Utopia]", valueDigest.digests.keys.toString())
    }

    @Test
    fun checkDigests() {
        val digestsCBOR: CBORObject = CBORObject.DecodeFromBytes(digestsByteString.toByteArray())

        assertEquals(digestsCBOR[1].toString(),
                valueDigest.digests.getValue("org.iso.18013.5.1").getValue(1).toHexString())

        assertEquals(digestsCBOR[2].toString(),
                valueDigest.digests.getValue("org.iso.18013.5.1").getValue(2).toHexString())

        assertEquals(digestsCBOR[3].toString(),
                valueDigest.digests.getValue("org.iso.18013.5.1").getValue(3).toHexString())
    }


}