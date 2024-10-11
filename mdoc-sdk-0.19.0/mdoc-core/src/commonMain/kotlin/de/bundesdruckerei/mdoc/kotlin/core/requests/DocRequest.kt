/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.requests

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.DocRequests
import de.bundesdruckerei.mdoc.kotlin.core.ItemsRequestBytes
import de.bundesdruckerei.mdoc.kotlin.core.common.DataKey.ITEMS_REQUEST
import de.bundesdruckerei.mdoc.kotlin.core.common.DataKey.READER_AUTH
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.readerauth.ReaderAuth

class DocRequest(val itemsRequestBytes: ItemsRequestBytes, val readerAuth: ReaderAuth?) :
    ICBORable {
    val itemsRequest: ItemsRequest
        get() = ItemsRequest.fromTaggedCBOR(itemsRequestBytes)

    override fun asCBOR(): CBORObject = CBORObject.NewMap().apply {
        Set(ITEMS_REQUEST.key, itemsRequestBytes)
        readerAuth?.let {
            Set(READER_AUTH.key, it.asCBOR())
        }
    }

    companion object {
        fun fromCBOR(cborObject: CBORObject): DocRequest {
            val itemsRequestCBOR = cborObject[ITEMS_REQUEST.key]

            val readerAuthCbor = cborObject[READER_AUTH.key]
            val readerAuth = readerAuthCbor?.let {
                ReaderAuth.fromCBOR(it).apply {
                }
            }

            return DocRequest(itemsRequestCBOR, readerAuth)
        }
    }
}

fun DocRequests.asCBOR(): CBORObject = CBORObject.NewArray().apply {
    forEach { Add(it.asCBOR()) }
}
