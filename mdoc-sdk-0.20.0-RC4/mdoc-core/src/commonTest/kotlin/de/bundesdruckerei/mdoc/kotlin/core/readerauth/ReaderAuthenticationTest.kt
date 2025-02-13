/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.readerauth

import com.google.common.truth.Truth.assertThat
import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.SessionTranscript
import de.bundesdruckerei.mdoc.kotlin.test.Data
import de.bundesdruckerei.mdoc.kotlin.core.common.CBOR_TAG_BYTE_STRING
import de.bundesdruckerei.mdoc.kotlin.core.readerauth.ReaderAuthentication.Companion.READER_AUTHENTICATION
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ReaderAuthenticationTest {

    private lateinit var sut: ReaderAuthentication
    private lateinit var cborUntagged: CBORObject
    private lateinit var sessionTranscript: SessionTranscript

    @Before
    fun setUp() {
        cborUntagged =
            CBORObject.DecodeFromBytes(Data.ReaderAuthentication.cborObjTagged.GetByteString())
        sessionTranscript =
            SessionTranscript.fromTaggedCBOR(Data.SessionTranscript.cborObjTagged)

        sut = ReaderAuthentication(
            sessionTranscript,
            Data.ItemsRequest.cborObjWithRequestInfoTagged
        )
    }

    @Test
    fun asCBOR() {
        sut = ReaderAuthentication(sessionTranscript, Data.ItemsRequest.cborObjWithRequestInfoTagged)
        assertThat(cborUntagged).isEqualTo(sut.asCBOR())
    }

    @Test
    fun asTaggedCbor() {
        sut = ReaderAuthentication(sessionTranscript, Data.ItemsRequest.cborObjWithRequestInfoTagged)
        assertThat(Data.ReaderAuthentication.cborObjTagged).isEqualTo(sut.asTaggedCBOR())
    }

    @Test
    fun fromTaggedCBOR() {
        sut = ReaderAuthentication.fromTaggedCBOR(Data.ReaderAuthentication.cborObjTagged)
        assertThat(sut.sessionTranscript).isEqualTo(sessionTranscript)
        assertThat(sut.itemsRequestBytes).isEqualTo(Data.ItemsRequest.cborObjWithRequestInfoTagged)
    }

    @Test
    fun `initializer block throw IllegalArgumentException for untagged ItemsRequestBytes`() {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            ReaderAuthentication(sessionTranscript, Data.ItemsRequest.cborObj)
        }
        assertThat(actual).hasMessageThat().contains("Missing tag $CBOR_TAG_BYTE_STRING in ItemsRequestBytes")
    }

    @Test
    fun `fromCBOR throw IllegalArgumentException for missing header`() {
        cborUntagged.set(0, CBORObject.FromObject("JunkData"))
        val actual = assertThrows(IllegalArgumentException::class.java) {
            ReaderAuthentication.fromCBOR(cborUntagged)
        }
        assertThat(actual).hasMessageThat().contains("Missing $READER_AUTHENTICATION header")
    }
}
