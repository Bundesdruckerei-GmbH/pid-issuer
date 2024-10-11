/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci.proofs.cwt

import COSE.OneKey
import COSE.Sign1Message
import COSE.sign
import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import de.bdr.openid4vc.common.Sign1Message
import de.bdr.openid4vc.common.signing.Signer
import java.lang.Exception
import java.lang.IllegalStateException
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Date

/**
 * Rudimentary cwt implementation with only the parts needed for CwtProofs.
 *
 * Only parses Sign1 messages.
 */
class Cwt {

    companion object {

        const val CWT_TAG = 61

        object Claims {
            val ISS = CBORObject.FromObject(1)
            val AUD = CBORObject.FromObject(3)
            val IAT = CBORObject.FromObject(6)
            val NONCE = CBORObject.FromObject(10)
        }

        object HeaderKeys {
            val ALG = CBORObject.FromObject(1)
            val COSE_KEY = CBORObject.FromObject("COSE_Key")
            val CTY = CBORObject.FromObject(3)
        }

        fun fromBytes(encoded: ByteArray): Cwt {
            val cbor = CBORObject.DecodeFromBytes(encoded)

            if (cbor.mostOuterTag.ToInt32Unchecked() == CWT_TAG) {
                cbor.UntagOne()
            }

            return fromSign1Message(Sign1Message(cbor))
        }

        fun fromSign1Message(sign1: Sign1Message) =
            Cwt().apply {
                this.sign1 = sign1
                val message = CBORObject.DecodeFromBytes(sign1.GetContent())
                message.keys.forEach { key -> claims[key] = message[key] }
                sign1.unprotectedAttributes.keys.forEach { key ->
                    unprotectedHeader[key] = sign1.unprotectedAttributes[key]
                }
                sign1.protectedAttributes.keys.forEach { key ->
                    protectedHeader[key] = sign1.protectedAttributes[key]
                }
            }
    }

    fun toBytes(signer: Signer, useCwtTag: Boolean = false): ByteArray {
        var coseObj = sign(signer).EncodeToCBORObject()
        if (useCwtTag) coseObj = coseObj.WithTag(CWT_TAG)
        return coseObj.EncodeToBytes()
    }

    fun sign(signer: Signer): Sign1Message {
        val message = Sign1Message()
        unprotectedHeader.keys.forEach { key ->
            message.unprotectedAttributes[key] = unprotectedHeader[key]
        }
        protectedHeader.keys.forEach { key ->
            message.protectedAttributes[key] = protectedHeader[key]
        }
        message.SetContent(claims.EncodeToBytes())
        message.sign(signer)
        sign1 = message
        return message
    }

    private var sign1: Sign1Message? = null

    val unprotectedHeader = CBORObject.NewMap()

    val protectedHeader = CBORObject.NewMap()

    val claims = CBORObject.NewMap()

    // TODO what encoding to use for nonce => byte
    var nonce: String?
        get() {
            val nonce = claims[Claims.NONCE] ?: return null
            check(nonce.type == CBORType.ByteString) { "nonce is not a byte string" }
            return nonce.GetByteString().toString(UTF_8)
        }
        set(value) {
            if (value == null) {
                claims.Remove(Claims.NONCE)
            } else {
                claims[Claims.NONCE] = CBORObject.FromObject(value.toByteArray(UTF_8))
            }
        }

    var coseKey: OneKey?
        get() {
            val rawKey = protectedHeader[HeaderKeys.COSE_KEY] ?: return null
            // TODO why is this a byte string instead of the COSE_Key directly
            check(rawKey.type == CBORType.ByteString) { "COSE_Key is not a byte string" }
            return try {
                OneKey(CBORObject.DecodeFromBytes(rawKey.GetByteString()))
            } catch (e: Exception) {
                throw IllegalStateException("COSE_Key invalid", e)
            }
        }
        set(value) {
            if (value == null) {
                protectedHeader.Remove(HeaderKeys.COSE_KEY)
            } else {
                protectedHeader[HeaderKeys.COSE_KEY] =
                    CBORObject.FromObject(value.AsCBOR().EncodeToBytes())
            }
        }

    var cty: String?
        get() {
            val cty = protectedHeader[HeaderKeys.CTY] ?: return null
            check(cty.type == CBORType.TextString) { "cty is not a string" }
            return cty.AsString()
        }
        set(value) {
            if (value == null) {
                protectedHeader.Remove(HeaderKeys.CTY)
            } else {
                protectedHeader[HeaderKeys.CTY] = CBORObject.FromObject(value)
            }
        }

    var alg: CBORObject?
        get() {
            return protectedHeader[HeaderKeys.ALG]
        }
        set(value) {
            if (value == null) {
                protectedHeader.Remove(HeaderKeys.ALG)
            } else {
                protectedHeader[HeaderKeys.ALG] = value
            }
        }

    var iss: String?
        get() {
            val aud = claims[Claims.ISS] ?: return null
            check(aud.type == CBORType.TextString) { "iss is not a text string" }
            return aud.AsString()
        }
        set(value) {
            if (value == null) {
                claims.Remove(Claims.ISS)
            } else {
                claims[Claims.ISS] = CBORObject.FromObject(value)
            }
        }

    var aud: String?
        get() {
            val aud = claims[Claims.AUD] ?: return null
            check(aud.type == CBORType.TextString) { "aud is not a text string" }
            return aud.AsString()
        }
        set(value) {
            if (value == null) {
                claims.Remove(Claims.AUD)
            } else {
                claims[Claims.AUD] = CBORObject.FromObject(value)
            }
        }

    var iat: Date?
        get() {
            val iat = claims[Claims.IAT] ?: return null
            check(iat.type == CBORType.Integer) { "iat is not an integer" }
            return Date(iat.AsInt64Value() * 1000)
        }
        set(value) {
            if (value == null) {
                claims.Remove(Claims.IAT)
            } else {
                claims[Claims.IAT] = CBORObject.FromObject(value.time / 1000)
            }
        }

    fun validate(key: OneKey) = (sign1 ?: error("Cwt not signed")).validate(key)
}
