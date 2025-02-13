/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.common

import com.upokecenter.cbor.CBORObject

internal const val CBOR_TAG_BYTE_STRING = 24

/**
 * CBor tag #6.1004 (tstr) for the default FORMAT pattern: "yyyy-MM-dd"
 */
const val CBOR_TAG_FULL_DATE = 1004uL

/**
 * Throws an IllegalArgumentException if this CBORObject does not have the
 * outermost tag CBOR_TAG_BYTE_STRING otherwise the function return a new
 * CBOR object from the backing byte array used in this CBOR object.
 * @return new CBOR object from the backing byte array of CBOR-encoded
 * bytes within the given CBOR object
 */
fun CBORObject.dataItem(): CBORObject {
    require(HasMostOuterTag(CBOR_TAG_BYTE_STRING)) { "Missing tag $CBOR_TAG_BYTE_STRING" }
    return CBORObject.DecodeFromBytes(GetByteString())
}

/**
 * Uses the CBOR.isNull() function to determine whether this CBOR object is a
 * CBOR null value (whether tagged or not) and returns null in this case.
 * @return null if this value is a CBOR null value; otherwise, this
 */
fun CBORObject.ifNotNull(): CBORObject? = if (this.isNull) null else this
