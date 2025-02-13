/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.crypto.cose

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable

/**
 * Class which is used for manually creating COSE_Sign1 object
 *
 */
class Sign1(val signature: ByteArray) : ICBORable {

    private val protectedHeader = CBORObject.NewMap().Set(1, -7)
    private val unprotectedHeader = CBORObject.NewMap()
    private val payload = CBORObject.Null

    fun asBytes(): ByteArray = asCBOR().EncodeToBytes()

    override fun asCBOR(): CBORObject {
        return CBORObject.NewArray().apply {
            Add(protectedHeader.EncodeToBytes())
            Add(unprotectedHeader)
            Add(payload)
            Add(signature)
        }
    }
}
