/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.requests

import com.google.common.truth.Truth.assertThat
import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.test.Data
import de.bundesdruckerei.mdoc.kotlin.core.common.DataKey
import org.junit.Before
import org.junit.Test

class DocRequestTest {

    private lateinit var sut: DocRequest

    @Before
    fun setUp() {
        // TODO add tests with ReaderAuth obj
        sut = DocRequest(Data.ItemsRequest.cborObjTagged, null)
    }

    @Test
    fun asCBOR() {
        // w/o readerAuth
        val expectedCBOR: CBORObject = CBORObject.NewMap().apply {
            Set(DataKey.ITEMS_REQUEST.key, Data.ItemsRequest.cborObjTagged)
        }

        assertThat(expectedCBOR).isEqualTo(sut.asCBOR())
    }

    @Test
    fun fromCBOR() {
        // w/o readerAuth
        sut = DocRequest.fromCBOR(Data.DocRequest.cborObj)
        assertThat(sut.readerAuth).isNull()
        assertThat(sut.itemsRequestBytes).isEqualTo(Data.ItemsRequest.cborObjTagged)
        assertThat(sut.itemsRequest.asTaggedCBOR()).isEqualTo(Data.ItemsRequest.cborObjTagged)
    }
}