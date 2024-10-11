/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.requests

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.DataElement
import de.bundesdruckerei.mdoc.kotlin.core.DocType
import de.bundesdruckerei.mdoc.kotlin.core.IntentToRetain
import de.bundesdruckerei.mdoc.kotlin.core.ItemsRequestBytes
import de.bundesdruckerei.mdoc.kotlin.core.NameSpaces
import de.bundesdruckerei.mdoc.kotlin.core.RequestInfo
import de.bundesdruckerei.mdoc.kotlin.core.any
import de.bundesdruckerei.mdoc.kotlin.core.common.CBOR_TAG_BYTE_STRING
import de.bundesdruckerei.mdoc.kotlin.core.common.DataKey.DOC_TYPE
import de.bundesdruckerei.mdoc.kotlin.core.common.DataKey.NAME_SPACES
import de.bundesdruckerei.mdoc.kotlin.core.common.DataKey.REQUEST_INFO
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.common.dataItem
import de.bundesdruckerei.mdoc.kotlin.core.tstr

class ItemsRequest(
    val docType: DocType,
    val nameSpaces: NameSpaces,
    val requestInfo: RequestInfo?,
    val raw: CBORObject? = null
) : ICBORable {

    override fun asCBOR(): CBORObject = CBORObject.NewMap().apply {
        Set(DOC_TYPE.key, docType)
        Set(NAME_SPACES.key, nameSpaces)
        requestInfo?.let { Set(REQUEST_INFO.key, it) }
    }

    // #6.24(bstr .cbor ItemsRequest)
    fun asTaggedCBOR(): ItemsRequestBytes =
        raw ?: CBORObject.FromObjectAndTag(asCBOR().EncodeToBytes(), CBOR_TAG_BYTE_STRING)

    fun asBytes(): ByteArray = asTaggedCBOR().EncodeToBytes()

    companion object {
        fun fromTaggedCBOR(cborObject: ItemsRequestBytes): ItemsRequest =
            fromCBOR(cborObject, cborObject.dataItem())

        fun fromCBOR(raw: CBORObject, cborObject: CBORObject): ItemsRequest {
            val docType: DocType = cborObject[DOC_TYPE.key].AsString()

            val nameSpacesCBOR = cborObject[NAME_SPACES.key]
            val nameSpaces: NameSpaces = mutableMapOf()

            for ((nameSpace, dataElements) in nameSpacesCBOR.entries) {
                val deMap = mutableMapOf<DataElement, IntentToRetain>()

                for ((element, intent) in dataElements.entries) {
                    deMap[element.AsString()] = intent.AsBoolean()
                }

                nameSpaces[nameSpace.AsString()] = deMap
            }

            val requestInfo: RequestInfo? = cborObject[REQUEST_INFO.key]?.run {
                mutableMapOf<tstr, any>().also {
                    entries.forEach { (key, value) ->
                        it[key.AsString()] = value
                    }
                }
            }

            return ItemsRequest(docType, nameSpaces, requestInfo, raw)
        }
    }
}
