/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.crypto.cose

import COSE.MAC0Message
import com.upokecenter.cbor.CBORObject

class COSEMac0(emitTag: Boolean = false, emitContent: Boolean = false) : MAC0Message() {

    init {
        this.emitTag = emitTag
        this.emitContent = emitContent
    }

    /**
     * Necessary fix for a bug in base class
     *
     * @see MAC0Message.EncodeCBORObject
     *
     * which disregards emitContent param and always emits content, non-compliant with RFC8152#6.2
     *
     * COSE_Mac0 = [
     *       Headers,
     *       payload : bstr / nil,
     *       tag : bstr,
     *    ]
     *
     * @see <a href="https://tools.ietf.org/html/rfc8152#section-6.2">
     */
    override fun EncodeCBORObject(): CBORObject {
        return super.EncodeCBORObject().apply {
            if (!emitContent) this[2] = CBORObject.Null
        }
    }

    companion object {
        fun fromCBOR(obj: CBORObject): COSEMac0 {
            return COSEMac0().apply {
                DecodeFromCBORObject(obj)
            }
        }
    }
}
