/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.test.Data
import de.bundesdruckerei.mdoc.kotlin.toHexString
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class IssuerSignedItemCBORTest(private val arrayIndex: Int) {

    private lateinit var issuerSignedItems: List<IssuerSignedItem>

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "element {index}, value {0}")
        fun data(): Collection<Int> {
            return listOf(0, 1, 2)
        }
    }

    @Before
    fun setUp() {
        IssuerSignedItemCBORTest(arrayIndex)
        issuerSignedItems = Data.IssuerSignedItem.cborItems.map { IssuerSignedItem.fromCBOR(it) }
    }

    @Test
    fun cborItems() {
        val t = Data.IssuerSignedItem.cborItems.map { IssuerSignedItem.fromCBOR(it) }
        t.asCBOR()
    }

    @Test
    fun asCBOR() {
        assertEquals(
            Data.IssuerSignedItem.cborItems[arrayIndex].get("random"),
            issuerSignedItems[arrayIndex].asCBOR().get("random")
        )
        assertEquals(
            Data.IssuerSignedItem.cborItems[arrayIndex].get("digestID"),
            issuerSignedItems[arrayIndex].asCBOR().get("digestID")
        )
        assertEquals(
            Data.IssuerSignedItem.cborItems[arrayIndex].get("elementIdentifier"),
            issuerSignedItems[arrayIndex].asCBOR().get("elementIdentifier")
        )
        assertEquals(
            Data.IssuerSignedItem.cborItems[arrayIndex].get("elementValue"),
            issuerSignedItems[arrayIndex].asCBOR().get("elementValue")
        )
    }

    @Test
    fun asTaggedCBORBytes() {
        val issuerSignedItemsExpectedTaggedCBOR: List<CBORObject> = issuerSignedItems.map {
            CBORObject.DecodeFromBytes(
                it.asTaggedCBORBytes().GetByteString()
            )
        }

        assertEquals(
            issuerSignedItems[arrayIndex].random.toHexString(),
            issuerSignedItemsExpectedTaggedCBOR[arrayIndex].get("random").toString()
        )
        assertEquals(
            issuerSignedItems[arrayIndex].digestID.toString(),
            issuerSignedItemsExpectedTaggedCBOR[arrayIndex].get("digestID").toString()
        )
        assertEquals(
            CBORObject.FromObject(issuerSignedItems[arrayIndex].elementIdentifier),
            issuerSignedItemsExpectedTaggedCBOR[arrayIndex].get("elementIdentifier")
        )
        assertEquals(
            issuerSignedItems[arrayIndex].elementValue,
            issuerSignedItemsExpectedTaggedCBOR[arrayIndex].get("elementValue")
        )
    }
}