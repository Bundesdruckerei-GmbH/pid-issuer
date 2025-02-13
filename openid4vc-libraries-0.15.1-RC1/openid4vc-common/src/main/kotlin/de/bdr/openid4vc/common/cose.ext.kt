/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common

import COSE.OneKey
import COSE.Sign1Message
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.upokecenter.cbor.CBORObject
import java.lang.IllegalArgumentException
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

fun OneKey.toJwk(): JWK {
    val pub =
        AsPublicKey() as? ECPublicKey ?: throw IllegalArgumentException("Unsupported key type")
    val priv = AsPrivateKey() as ECPrivateKey?
    return ECKey.Builder(Curve.forECParameterSpec(pub.params), pub).privateKey(priv).build()
}

const val SIGN1_TAG = 18

fun Sign1Message(cbor: CBORObject): Sign1Message = CborObjectBasedSign1Message(cbor)

/** Needed because DecodeFromCBORObject is protected */
internal class CborObjectBasedSign1Message(cbor: CBORObject) : Sign1Message() {
    init {
        if (cbor.mostOuterTag.ToInt32Unchecked() == SIGN1_TAG) {
            cbor.UntagOne()
        }
        DecodeFromCBORObject(cbor)
    }
}
