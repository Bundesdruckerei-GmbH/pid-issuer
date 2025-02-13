/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.requests

import com.google.common.truth.Truth.assertThat
import de.bundesdruckerei.mdoc.kotlin.test.Data
import org.junit.Test

class ItemsRequestTest {

    private lateinit var sut: ItemsRequest

    @Test
    fun asCBOR() {
        sut = ItemsRequest(
            Data.mdlDocType,
            mutableMapOf(Data.ItemsRequest.nameSpaceKey to Data.ItemsRequest.dataElements),
            null
        )

        assertThat(sut.asCBOR()).isEqualTo(Data.ItemsRequest.cborObj)

        sut = ItemsRequest(
            Data.mdlDocType,
            mutableMapOf(Data.ItemsRequest.nameSpaceKey to Data.ItemsRequest.dataElements),
            Data.ItemsRequest.requestInfo
        )

        assertThat(sut.asTaggedCBOR()).isEqualTo(Data.ItemsRequest.cborObjWithRequestInfoTagged)
    }

    @Test
    fun fromTaggedCBOR() {
        sut = ItemsRequest.fromTaggedCBOR(Data.ItemsRequest.cborObjTagged)
        verifyProperties(sut)

        sut = ItemsRequest.fromTaggedCBOR(Data.ItemsRequest.cborObjWithRequestInfoTagged)
        verifyProperties(sut, false)
    }

    @Test
    fun fromCBOR() {
        sut = ItemsRequest.fromCBOR(Data.ItemsRequest.cborObj, Data.ItemsRequest.cborObj)
        verifyProperties(sut)
    }

    private fun verifyProperties(sut: ItemsRequest, isRequestInfoNull: Boolean = true) {
        assertThat(sut.docType).isEqualTo(Data.mdlDocType)
        assertThat(sut.nameSpaces[Data.ItemsRequest.nameSpaceKey]).isNotNull()
        assertThat(sut.nameSpaces[Data.ItemsRequest.nameSpaceKey]).isEqualTo(Data.ItemsRequest.dataElements)
        if (isRequestInfoNull)
            assertThat(sut.requestInfo).isNull()
        else
            assertThat(sut.requestInfo).isEqualTo(Data.ItemsRequest.requestInfo)
    }
}
