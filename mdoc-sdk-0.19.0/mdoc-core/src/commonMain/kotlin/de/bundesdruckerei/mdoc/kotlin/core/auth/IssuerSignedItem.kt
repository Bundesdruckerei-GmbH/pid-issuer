/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.DataElementIdentifier
import de.bundesdruckerei.mdoc.kotlin.core.bstr
import de.bundesdruckerei.mdoc.kotlin.core.common.CBOR_TAG_BYTE_STRING
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.tstr
import de.bundesdruckerei.mdoc.kotlin.core.uint

class IssuerSignedItem(
    val digestID: uint,
    val random: bstr,
    val elementIdentifier: DataElementIdentifier,
    val elementValue: CBORObject
) : ICBORable {

    private var pristineCBORBytes: ByteArray? = null

    override fun asCBOR(): CBORObject = CBORObject.NewMap()
        .Set("digestID", digestID)
        .Set("random", random)
        .Set("elementIdentifier", elementIdentifier)
        .Set("elementValue", elementValue)

    fun asBytes(): ByteArray = pristineCBORBytes ?: asCBOR().EncodeToBytes()

    // #6.24 (bstr .cbor IssuerSignedItem)
    fun asTaggedCBORBytes(): CBORObject =
        CBORObject.FromObjectAndTag(asBytes(), CBOR_TAG_BYTE_STRING)

    companion object {
        fun fromCBOR(cborObject: CBORObject): IssuerSignedItem {
            val digestID = cborObject["digestID"].AsNumber().ToInt64Checked()
            val random = cborObject["random"].GetByteString()
            val elementIdentifier = cborObject["elementIdentifier"].AsString()
            val elementValue = cborObject["elementValue"]
            return IssuerSignedItem(digestID, random, elementIdentifier, elementValue)
        }

        fun fromCBOR(cborObject: CBORObject, pristineData: ByteArray): IssuerSignedItem =
            fromCBOR(cborObject).apply {
                pristineCBORBytes = pristineData
            }

        fun fromBytes(data: ByteArray): IssuerSignedItem =
            fromCBOR(CBORObject.DecodeFromBytes(data)).apply {
                pristineCBORBytes = data
            }

        fun fromTaggedCBOR(taggedCborObject: CBORObject): IssuerSignedItem =
            fromBytes(taggedCborObject.GetByteString())

        fun fromTaggedBytes(data: ByteArray): IssuerSignedItem =
            fromTaggedCBOR(CBORObject.DecodeFromBytes(data))
    }
}

typealias IssuerSignedItems = List<IssuerSignedItem>

fun IssuerSignedItems.asCBOR(): CBORObject = CBORObject.NewArray().also { cborObject ->
    this.forEach {
        cborObject.Add(it.asTaggedCBORBytes())
    }
}

fun IssuerSignedItems.getItem(itemID: tstr): IssuerSignedItem? =
    firstOrNull { issuerSignedItem -> issuerSignedItem.elementIdentifier == itemID }
