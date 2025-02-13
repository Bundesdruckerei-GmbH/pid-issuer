/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.crypto.CryptoUtils
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.util.Collections

fun ByteArray.toHexString() = joinToString(prefix = "h'", separator = "", postfix = "'") { "%02X".format(it) }

fun sortIssuerSignedNameSpacesData(expected: CBORObject, actual: CBORObject): List<List<CBORObject>> {

    val issuerSignedCBORNameSpaces: List<CBORObject> = expected
            .get("nameSpaces")
            .get("org.iso.18013.5.1")
            .values
            .stream()
            .map { CBORObject.DecodeFromBytes(it.GetByteString()) }
            .toList()

    val modifiableIssuerSignedCBORNameSpaces: List<CBORObject> = ArrayList(issuerSignedCBORNameSpaces)
    Collections.sort(modifiableIssuerSignedCBORNameSpaces, Comparator<CBORObject> { o1: CBORObject, o2: CBORObject ->
        o1.get("digestID").compareTo(o2.get("digestID"))
    })

    val issuerSignedNameSpaces: List<CBORObject> = actual
            .get("nameSpaces")
            .get("org.iso.18013.5.1")
            .values
            .stream()
            .map { CBORObject.DecodeFromBytes(it.GetByteString()) }
            .toList()

    val modifiableIssuerSignedNameSpaces: List<CBORObject> = ArrayList(issuerSignedNameSpaces)

    Collections.sort(modifiableIssuerSignedNameSpaces, Comparator<CBORObject> { o1: CBORObject, o2: CBORObject ->
        o1.get("digestID").compareTo(o2.get("digestID"))
    })

    return listOf(modifiableIssuerSignedCBORNameSpaces, modifiableIssuerSignedNameSpaces)
}

fun generatePrivateKey(privateKeyNumber: BigInteger, curveName: String): ECPrivateKey {
    Security.addProvider(BouncyCastleProvider())

    val ecParameterSpec = ECNamedCurveTable.getParameterSpec(curveName)
    val keySpec = ECPrivateKeySpec(privateKeyNumber, ecParameterSpec)

    val keyFactory: KeyFactory = KeyFactory.getInstance(
        CryptoUtils.KEY_FACTORY_ALGORITHM,
        CryptoUtils.KEY_FACTORY_PROVIDER
    )
    val privateKey: PrivateKey = keyFactory.generatePrivate(keySpec)
    return privateKey as ECPrivateKey
}

fun generatePublicFromPrivateKey(privateKey: ECPrivateKey): PublicKey
{
    val keyFactory = KeyFactory.getInstance(CryptoUtils.KEY_FACTORY_ALGORITHM, CryptoUtils.KEY_FACTORY_PROVIDER)

    val d = privateKey.d

    val ecSpec = privateKey.parameters
    val q = privateKey.parameters.g.multiply(d)


    val pubSpec = org.bouncycastle.jce.spec.ECPublicKeySpec(q, ecSpec)
    return keyFactory.generatePublic(pubSpec)
}
