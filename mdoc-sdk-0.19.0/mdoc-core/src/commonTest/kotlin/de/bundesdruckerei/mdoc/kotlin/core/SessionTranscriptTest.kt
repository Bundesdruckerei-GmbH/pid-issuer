/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core

import com.google.common.truth.Truth.assertThat
import de.bundesdruckerei.mdoc.kotlin.core.common.dataItem
import de.bundesdruckerei.mdoc.kotlin.test.Data
import org.junit.Assert.assertThrows
import org.junit.Test

class SessionTranscriptTest {

    private lateinit var sut: SessionTranscript

    @Test
    fun `verify conditions within initializer block`() {
        sut = SessionTranscript(
            deviceEngagementBytes = Data.SessionTranscript.deviceEngagementBytes,
            eReaderKeyBytes = Data.SessionTranscript.eReaderKeyBytes,
            handoverBytes = Data.SessionTranscript.handoverBytes
        )

        sut = SessionTranscript(
            deviceEngagementBytes = Data.SessionTranscript.deviceEngagementBytes,
            eReaderKeyBytes = Data.SessionTranscript.eReaderKeyBytes,
            handoverBytes = null
        )

        sut = SessionTranscript(
            deviceEngagementBytes = null,
            eReaderKeyBytes = null,
            handoverBytes = Data.SessionTranscript.handoverBytes
        )

        sut = SessionTranscript(
            deviceEngagementBytes = null,
            eReaderKeyBytes = Data.SessionTranscript.eReaderKeyBytes,
            handoverBytes = Data.SessionTranscript.handoverBytes
        )

        sut = SessionTranscript(
            deviceEngagementBytes = Data.SessionTranscript.deviceEngagementBytes,
            eReaderKeyBytes = null,
            handoverBytes = Data.SessionTranscript.handoverBytes
        )

        val expectedMsg = "Invalid state of SessionTranscript arguments detected."
        var actualException = assertThrows(IllegalArgumentException::class.java) {
            sut = SessionTranscript(
                deviceEngagementBytes = null,
                eReaderKeyBytes = null,
                handoverBytes = null
            )
        }
        assertThat(actualException).hasMessageThat().contains(expectedMsg)

        actualException = assertThrows(IllegalArgumentException::class.java) {
            sut = SessionTranscript(
                deviceEngagementBytes = Data.SessionTranscript.deviceEngagementBytes,
                eReaderKeyBytes = Data.SessionTranscript.eReaderKeyBytes,
                handoverBytes = ByteArray(0)
            )
        }
        assertThat(actualException).hasMessageThat().contains("HandoverBytes")
    }


    @Test
    fun `sessionTranscript with invalid eReaderKeyBytes (missing COSE_Key structure)`() {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            sut = SessionTranscript(
                deviceEngagementBytes = Data.SessionTranscript.deviceEngagementBytes,
                eReaderKeyBytes = Data.SessionTranscript.invalidEReaderKeyBytes,
                handoverBytes = Data.SessionTranscript.handoverBytes
            )
        }
        assertThat(actual).hasMessageThat().endsWith("eReaderKeyBytes with invalid structure.")
    }

    @Test
    fun `verify fromTaggedCBOR`() {
        sut = SessionTranscript.fromTaggedCBOR(Data.SessionTranscript.cborObjTagged)

        assertThat(sut).isNotNull()
        assertThat(sut.eReaderKeyBytes).isEqualTo(Data.SessionTranscript.eReaderKeyBytes)
        assertThat(sut.handoverBytes).isEqualTo(Data.SessionTranscript.handoverBytes)
        assertThat(sut.deviceEngagementBytes).isEqualTo(Data.SessionTranscript.deviceEngagementBytes)
    }

    @Test
    fun `verify fromTaggedCBOR without deviceEngagementBytes and eReaderKeyBytes`() {
        sut = SessionTranscript.fromTaggedCBOR(Data.SessionTranscript.cborObjWithHandoverOnly)

        assertThat(sut).isNotNull()
        assertThat(sut.eReaderKeyBytes).isNull()
        assertThat(sut.deviceEngagementBytes).isNull()
        assertThat(sut.handoverBytes).isEqualTo(Data.SessionTranscript.handoverBytes)
    }

    @Test
    fun `verify fromTaggedCBOR without handover`() {
        sut = SessionTranscript.fromTaggedCBOR(Data.SessionTranscript.cborObjWithoutHandover)

        assertThat(sut).isNotNull()
        assertThat(sut.eReaderKeyBytes).isEqualTo(Data.SessionTranscript.eReaderKeyBytes)
        assertThat(sut.deviceEngagementBytes).isEqualTo(Data.SessionTranscript.deviceEngagementBytes)
        assertThat(sut.handoverBytes).isNull()
    }

    @Test
    fun `verify fromCBOR`() {
        sut = SessionTranscript.fromCBOR(Data.SessionTranscript.cborObjTagged.dataItem())

        assertThat(sut).isNotNull()
        assertThat(sut.eReaderKeyBytes).isEqualTo(Data.SessionTranscript.eReaderKeyBytes)
        assertThat(sut.deviceEngagementBytes).isEqualTo(Data.SessionTranscript.deviceEngagementBytes)
        assertThat(sut.handoverBytes).isEqualTo(Data.SessionTranscript.handoverBytes)
    }

    @Test
    fun `verify asCBOR`() {
        sut = SessionTranscript(
            deviceEngagementBytes = Data.SessionTranscript.deviceEngagementBytes,
            eReaderKeyBytes = Data.SessionTranscript.eReaderKeyBytes,
            handoverBytes = Data.SessionTranscript.handoverBytes
        )

        val actual = sut.asCBOR()
        assertThat(actual).isEqualTo(Data.SessionTranscript.cborObjTagged.dataItem())
    }

    @Test
    fun `verify asCBOR without deviceEngagementBytes and eReaderKeyBytes`() {
        sut = SessionTranscript(
            deviceEngagementBytes = null,
            eReaderKeyBytes = null,
            handoverBytes = Data.SessionTranscript.handoverBytes
        )

        val actual = sut.asCBOR()
        assertThat(actual).isEqualTo(Data.SessionTranscript.cborObjWithHandoverOnly.dataItem())
    }

    @Test
    fun `verify asCBOR without deviceEngagementBytes`() {
        sut = SessionTranscript(
            deviceEngagementBytes = null,
            eReaderKeyBytes = Data.SessionTranscript.eReaderKeyBytes,
            handoverBytes = Data.SessionTranscript.handoverBytes
        )

        val actual = sut.asCBOR()
        assertThat(actual).isEqualTo(Data.SessionTranscript.cborObjWithoutDeviceEngagementBytes.dataItem())
    }

    @Test
    fun `verify asCBOR without handover`() {
        sut = SessionTranscript(
            deviceEngagementBytes = Data.SessionTranscript.deviceEngagementBytes,
            eReaderKeyBytes = Data.SessionTranscript.eReaderKeyBytes,
            handoverBytes = null
        )

        val actual = sut.asCBOR()
        assertThat(actual).isEqualTo(Data.SessionTranscript.cborObjWithoutHandover.dataItem())
    }

    @Test
    fun `verify asTaggedCBOR`() {
        sut = SessionTranscript(
            deviceEngagementBytes = Data.SessionTranscript.deviceEngagementBytes,
            eReaderKeyBytes = Data.SessionTranscript.eReaderKeyBytes,
            handoverBytes = Data.SessionTranscript.handoverBytes
        )

        val actual = sut.asTaggedCBOR()
        assertThat(actual).isEqualTo(Data.SessionTranscript.cborObjTagged)
    }

    @Test
    fun `verify equals and hashcode function`() {
        sut = SessionTranscript(
            deviceEngagementBytes = Data.SessionTranscript.deviceEngagementBytes,
            eReaderKeyBytes = Data.SessionTranscript.eReaderKeyBytes,
            handoverBytes = Data.SessionTranscript.handoverBytes
        )
        var cbor = sut.asTaggedCBOR()
        var actual = SessionTranscript.fromTaggedCBOR(cbor)
        assertThat(actual).isEqualTo(sut)
        assertThat(actual.hashCode()).isEqualTo(1966560748)

        sut = SessionTranscript(
            deviceEngagementBytes = Data.SessionTranscript.deviceEngagementBytes,
            eReaderKeyBytes = Data.SessionTranscript.eReaderKeyBytes,
            handoverBytes = null
        )
        cbor = sut.asTaggedCBOR()
        actual = SessionTranscript.fromTaggedCBOR(cbor)
        assertThat(actual).isEqualTo(sut)
        assertThat(actual.hashCode()).isEqualTo(-1786290414)

        sut = SessionTranscript(
            deviceEngagementBytes = null,
            eReaderKeyBytes = null,
            handoverBytes = Data.SessionTranscript.handoverBytes
        )
        cbor = sut.asTaggedCBOR()
        actual = SessionTranscript.fromTaggedCBOR(cbor)
        assertThat(actual).isEqualTo(sut)
        assertThat(actual.hashCode()).isEqualTo(-542116134)

        sut = SessionTranscript(
            deviceEngagementBytes = null,
            eReaderKeyBytes = Data.SessionTranscript.eReaderKeyBytes,
            handoverBytes = Data.SessionTranscript.handoverBytes
        )
        cbor = sut.asTaggedCBOR()
        actual = SessionTranscript.fromTaggedCBOR(cbor)
        assertThat(actual).isEqualTo(sut)
        assertThat(actual.hashCode()).isEqualTo(1101466060)

        sut = SessionTranscript(
            deviceEngagementBytes = Data.SessionTranscript.deviceEngagementBytes,
            eReaderKeyBytes = null,
            handoverBytes = Data.SessionTranscript.handoverBytes
        )
        cbor = sut.asTaggedCBOR()
        actual = SessionTranscript.fromTaggedCBOR(cbor)
        assertThat(actual).isEqualTo(sut)
        assertThat(actual.hashCode()).isEqualTo(322978554)
    }
}