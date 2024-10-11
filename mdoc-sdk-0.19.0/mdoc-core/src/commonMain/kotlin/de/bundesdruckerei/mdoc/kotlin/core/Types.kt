/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core

import COSE.OneKey
import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import de.bundesdruckerei.mdoc.kotlin.core.requests.DocRequest
import java.util.Date

@Suppress("EnumNaming")
enum class Types(val cborType: CBORType, val tag: uint? = null) {
    tstr(CBORType.TextString),
    fullDate(CBORType.TextString, 18013),
    tdate(CBORType.TextString, 0)
}

typealias uint = Long
typealias int = Int
typealias tstr = String
typealias bstr = ByteArray
typealias tdate = Date
typealias DocType = tstr
typealias DocTypes = ArrayList<DocType>
typealias NameSpace = tstr
typealias any = Any
typealias DataElementIdentifier = tstr
typealias DataElementValue = any

typealias DocRequests = Array<DocRequest>

typealias MobileeIDdocuments = Array<MobileeIDdocument>

typealias DataElement = tstr
typealias IntentToRetain = Boolean
typealias DataElements = MutableMap<DataElement, IntentToRetain>
typealias NameSpaces = MutableMap<NameSpace, DataElements>
typealias RequestInfo = MutableMap<tstr, any>
typealias ItemsRequestBytes = CBORObject // #6.24(bstr .cbor ItemsRequest)

typealias DataItemNames = Array<tstr>
typealias MacKeys = Array<COSEKey>

typealias DeviceEngagementBytes = bstr // #6.24 (bstr .cbor DeviceEngagement)
typealias EReaderKeyBytes = bstr // #6.24 (bstr .cbor COSE_Key)
typealias COSEKey = OneKey

typealias DigestIDs = MutableMap<uint, bstr>
typealias NameSpacesDigests = Map<NameSpace, DigestIDs>
