/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.bstr
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.tstr
import de.bundesdruckerei.mdoc.kotlin.core.uint

/**
 * Status claim according to https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/06/
 * chapter 6.3.
 */
data class StatusClaim(val info: StatusListInfo) : ICBORable {
    constructor(cborObject: CBORObject) : this(
        StatusListInfo(cborObject[StatusListInfo.KEY])
    )

    override fun asCBOR(): CBORObject = CBORObject.NewMap().apply {
        Set(StatusListInfo.KEY, info.asCBOR())
    }

    companion object {
        internal const val KEY = "status"
    }
}

data class StatusListInfo(
    val idx: uint,
    val uri: tstr,
    val certificate: bstr? = null
) : ICBORable {
    constructor(cborObject: CBORObject) : this(
        idx = cborObject[IDX].AsInt64Value(),
        uri = cborObject[URI].AsString(),
        certificate = cborObject[CERTIFICATE]?.run { GetByteString() }
    )

    override fun asCBOR(): CBORObject = CBORObject.NewMap().apply {
        Set(IDX, idx)
        Set(URI, uri)
        certificate?.apply {
            Set(CERTIFICATE, this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StatusListInfo

        if (idx != other.idx) return false
        if (uri != other.uri) return false
        if (certificate != null) {
            if (other.certificate == null) return false
            if (!certificate.contentEquals(other.certificate)) return false
        } else if (other.certificate != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = idx.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + (certificate?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        private const val IDX = "idx"
        private const val URI = "uri"
        private const val CERTIFICATE = "certificate"
        internal const val KEY = "status_list"
    }
}
