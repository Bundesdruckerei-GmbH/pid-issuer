/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import com.google.common.truth.Truth.assertThat
import de.bundesdruckerei.mdoc.kotlin.test.Data
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DeviceAuthTest {

    private lateinit var deviceAuth: DeviceAuth

    @Before
    fun setUp() {
        deviceAuth = DeviceAuth.fromCBOR(Data.DeviceAuth.cborObjTagged)
    }

    @Test
    fun asCBOR() {
        assertEquals(Data.DeviceAuth.cborObjTagged, deviceAuth.asCBOR())
    }

    @Test
    fun fromCBOR() {
        assertEquals(
            Data.DeviceAuth.cborObjTagged,
            DeviceAuth.fromCBOR(Data.DeviceAuth.cborObjTagged).asCBOR()
        )
    }

    @Test
    fun `fromCBOR throw IllegalArgumentException for missing ContextTag`() {
        val cborObj = Data.DeviceAuth.cborObjTagged
        cborObj.keys.remove(cborObj.keys.first())

        val actual = Assert.assertThrows(IllegalArgumentException::class.java) {
            DeviceAuth.fromCBOR(cborObj)
        }

        assertThat(actual).hasMessageThat().contains("ContextTag should not be null.")
    }


}
