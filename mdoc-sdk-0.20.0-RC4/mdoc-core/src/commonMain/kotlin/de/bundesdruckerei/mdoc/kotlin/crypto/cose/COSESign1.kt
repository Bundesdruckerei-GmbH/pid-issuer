/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.crypto.cose

import COSE.Sign1Message
import com.upokecenter.cbor.CBORObject

open class COSESign1(emitTag: Boolean = false, emitContent: Boolean = false) :
    Sign1Message(emitTag, emitContent) {

    /**
     * Necessary fix for a bug in base class
     * @see Sign1Message.EncodeCBORObject
     * which adds null instead of CBORObject.Null if emitContent is false,
     * causing Exception in toString() method. That behaviour is non-compliant with RFC8152#4.2
     * which allows payload to be nil.
     *    COSE_Sign1 = [
     *        Headers,
     *        payload : bstr / nil,
     *        signature : bstr
     *    ]
     * @see <a href="https://tools.ietf.org/html/rfc8152#section-4.2">
     *
     * Both CDDL spec appendix D <a href="https://tools.ietf.org/html/rfc8610#appendix-D" />
     * and CBOR spec section 2.3 <a href="https://tools.ietf.org/html/rfc7049#section-2.3" />
     * define nil / null / Null as simple value #7.22 (major type 7 with a value of 22)
     */
    override fun EncodeCBORObject(): CBORObject {
        return super.EncodeCBORObject().apply {
            if (!emitContent) this[2] = CBORObject.Null
        }
    }

    companion object {
        fun fromCBOR(obj: CBORObject): COSESign1 {
            return COSESign1().apply {
                DecodeFromCBORObject(obj)
            }
        }
    }
}
