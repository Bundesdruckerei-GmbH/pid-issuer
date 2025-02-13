/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.crypto.cose

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable

/**
 * Sig_structure from RFC8152 that holds values which will be signed
 *
 */
class SigStructure(val payload: ByteArray) : ICBORable {

    private val contextString = CBORObject.FromObject("Signature1")
    private val bodyProtected = CBORObject.NewMap().Set(1, -7)
    private val externalAad = CBORObject.DecodeFromBytes(byteArrayOf(0x40.toByte()))

    fun asBytes(): ByteArray = asCBOR().EncodeToBytes()

    override fun asCBOR(): CBORObject {
        return CBORObject.NewArray().apply {
            Add(contextString)
            Add(bodyProtected.EncodeToBytes())
            Add(externalAad)
            Add(payload)
        }
    }
}
