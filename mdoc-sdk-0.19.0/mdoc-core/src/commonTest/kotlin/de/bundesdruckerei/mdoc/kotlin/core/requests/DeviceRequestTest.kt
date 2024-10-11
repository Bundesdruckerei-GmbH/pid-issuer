/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.requests

import com.google.common.truth.Truth.assertThat
import de.bundesdruckerei.mdoc.kotlin.core.DocRequests
import de.bundesdruckerei.mdoc.kotlin.test.Data
import de.bundesdruckerei.mdoc.kotlin.core.common.IncompatibleVersionException
import de.bundesdruckerei.mdoc.kotlin.core.common.toHex
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class DeviceRequestTest {

    private lateinit var sut: DeviceRequest
    private lateinit var docRequest: DocRequest
    private lateinit var docRequests: DocRequests

    @Before
    fun setUp() {
        docRequest = DocRequest(Data.ItemsRequest.cborObjTagged, null)
        docRequests = DocRequests(1) { docRequest }
        docRequests[0] = docRequest
        sut = DeviceRequest(Data.DeviceRequest.version, docRequests)
    }

    @Test
    fun `invalid version detection`() {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            DeviceRequest("unknownVersion", emptyArray(), null)
        }
        assertThat(actual).hasMessageThat().contains("Invalid Version detected.")
    }

    @Test
    fun `incompatible version detection`() {
        val actual = assertThrows(IncompatibleVersionException::class.java) {
            DeviceRequest("${Int.MAX_VALUE}.0", emptyArray(), null)
        }
        assertThat(actual).hasMessageThat().contains("is not supported")
    }

    @Test
    fun `valid and supported version detected`() {
        val deviceRequest = DeviceRequest("1.0", emptyArray(), null)
        println(deviceRequest)
    }

    @Test
    fun fromCBOR() {
        sut = DeviceRequest.fromCBOR(Data.DeviceRequest.cborObjTagged)
        assertThat(sut.version).isEqualTo(Data.DeviceRequest.version)
        assertThat(sut.macKeys).isNull()
        assertThat(sut.docRequests.size).isEqualTo(1)
        assertThat(
            sut.docRequests.asCBOR().EncodeToBytes().toHex()
        ).isEqualTo(Data.DeviceRequest.docRequestArrayHex)
    }

    @Test
    fun asCBOR() {
        assertThat(sut.asCBOR().EncodeToBytes().toHex()).isEqualTo(Data.DeviceRequest.dataHex)
    }
}