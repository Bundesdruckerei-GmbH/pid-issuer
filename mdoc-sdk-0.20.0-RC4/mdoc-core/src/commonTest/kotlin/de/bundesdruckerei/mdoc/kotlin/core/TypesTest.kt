/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core

import com.upokecenter.cbor.CBORType
import org.junit.Assert.assertEquals
import org.junit.Test

class TypesTest {

    @Test
    fun getCborType() {
        assertEquals(CBORType.TextString, Types.tstr.cborType)
        assertEquals(CBORType.TextString, Types.fullDate.cborType)
        assertEquals(CBORType.TextString, Types.tdate.cborType)
    }

    @Test
    fun getTag() {
        assertEquals(null, Types.tstr.tag)
        assertEquals(18013L, Types.fullDate.tag)
        assertEquals(0L, Types.tdate.tag)
    }
}
