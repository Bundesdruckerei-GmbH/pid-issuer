/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import com.google.common.truth.Truth.assertThat
import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.test.Data
import de.bundesdruckerei.mdoc.kotlin.core.common.hexToByteArray
import de.bundesdruckerei.mdoc.kotlin.core.deviceauth.DeviceAuthentication.Companion.DEVICE_AUTHENTICATION
import de.bundesdruckerei.mdoc.kotlin.toHexString
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class DeviceAuthenticationTest {

    private lateinit var deviceAuthenticationCBORObject: CBORObject
    private lateinit var deviceAuthentication: DeviceAuthentication
    private val deviceNameSpacesByteString = "D81841A0"

    private lateinit var deviceNameSpacesCBORObject: CBORObject

    @Before
    fun setUp() {
        deviceAuthenticationCBORObject = CBORObject.DecodeFromBytes(Data.DeviceAuthentication.cborObjTagged.GetByteString())
        deviceAuthentication = DeviceAuthentication.fromTaggedCBOR(Data.DeviceAuthentication.cborObjTagged)
        deviceNameSpacesCBORObject = CBORObject.DecodeFromBytes(deviceNameSpacesByteString.hexToByteArray())
    }

    @Test
    fun asCBOR() {
        assertEquals(deviceAuthenticationCBORObject, deviceAuthentication.asCBOR())
    }

    @Test
    fun asTaggedCBOR() {
        assertEquals(Data.DeviceAuthentication.cborObjTagged, deviceAuthentication.asTaggedCBOR())
    }

    @Test
    fun asBytes() {
        assertEquals("h'${Data.DeviceAuthentication.dataHex.uppercase(Locale.getDefault())}'", deviceAuthentication.asBytes().toHexString())
    }

    @Test
    fun fromCBOR() {
        assertEquals(Data.SessionTranscript.cborObj, deviceAuthentication.sessionTranscript.asCBOR())
        assertEquals(deviceNameSpacesCBORObject, deviceAuthentication.deviceNameSpacesBytes)
        assertEquals(Data.mdlDocType, deviceAuthentication.docType)
    }


    @Test
    fun `fromCBOR throw IllegalArgumentException for missing header`() {
        val cborUntagged =  CBORObject.DecodeFromBytes(Data.DeviceAuthentication.cborObjTagged.GetByteString())
        cborUntagged.set(0, CBORObject.FromObject("JunkData"))
        val actual = Assert.assertThrows(IllegalArgumentException::class.java) {
            DeviceAuthentication.fromCBOR(cborUntagged)
        }
        assertThat(actual).hasMessageThat().contains("Missing $DEVICE_AUTHENTICATION header")
    }
}
