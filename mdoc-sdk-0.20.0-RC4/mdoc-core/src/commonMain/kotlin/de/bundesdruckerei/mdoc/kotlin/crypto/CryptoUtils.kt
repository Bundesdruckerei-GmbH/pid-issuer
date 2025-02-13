/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.crypto

import COSE.CoseException
import de.bundesdruckerei.mdoc.kotlin.core.common.log
import org.bouncycastle.crypto.EphemeralKeyPair
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil
import org.bouncycastle.util.BigIntegers
import java.nio.charset.StandardCharsets.UTF_8
import java.security.KeyPair
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import kotlin.experimental.and

object CryptoUtils {

    // Took some names from "OneKey.java" and some from here https://code.yawk.at/org.bouncycastle/bcprov-jdk15on/1.61/org/bouncycastle/crypto/tls/TlsECCUtils.java
    // Actually the string themselves are never used in the CBOR.

    const val CURVE_P_256 = "secp256r1"
    const val CURVE_P_384 = "secp384r1"
    const val CURVE_P_521 = "secp521r1"
    const val Ed_25519 = "Ed25519"
    const val Ed_448 = "Ed448"
    const val X_25519 = "X25519"
    const val X_448 = "X448"
    const val Brainpool_256 = "brainpoolP256r1"
    const val Brainpool_320 = "brainpoolP320r1"
    const val Brainpool_384 = "brainpoolP384r1"
    const val Brainpool_512 = "brainpoolP512r1"

    const val KEY_FACTORY_ALGORITHM = "EC"
    const val KEY_AGREEMENT_ALGORITHM = "ECDH"
    const val KEY_FACTORY_PROVIDER = "BC"

    // Signature algorithms used for mdoc authentication
    const val ES256 = "SHA256withECDSA"
    const val ES384 = "SHA384withECDSA"
    const val ES512 = "SHA512withECDSA"

    val skReaderInfo = "SKReader".toByteArray(UTF_8)
    val skDeviceInfo = "SKDevice".toByteArray(UTF_8)

    fun deriveSessionKey(publicKey: PublicKey, keyPair: KeyPair, salt: ByteArray?): ByteArray {
        val pubParam = ECUtil.generatePublicKeyParameter(publicKey)
        val prParam = ECUtil.generatePrivateKeyParameter(keyPair.private)

        return deriveSessionKeyFromParams(pubParam, prParam, salt)
    }

    fun deriveSessionKey(
        publicKey: PublicKey,
        ephKeyPair: EphemeralKeyPair,
        salt: ByteArray?
    ): ByteArray {
        val pubParam = ECUtil.generatePublicKeyParameter(publicKey)
        val prParam = ephKeyPair.keyPair.private

        return deriveSessionKeyFromParams(pubParam, prParam, salt)
    }

    fun deriveSessionKey(
        publicKey: PublicKey,
        privateKey: PrivateKey,
        salt: ByteArray?,
        info: ByteArray? = null,
        length: Int = 32
    ): ByteArray {
        val pubParam = ECUtil.generatePublicKeyParameter(publicKey)
        val prParam = ECUtil.generatePrivateKeyParameter(privateKey)

        return deriveSessionKeyFromParams(pubParam, prParam, salt, info, length)
    }

    fun deriveSessionKey(
        sharedSecret: ByteArray,
        salt: ByteArray?,
        info: ByteArray? = null,
        length: Int = 32
    ): ByteArray = deriveSessionKeyFromSharedSecret(sharedSecret, salt, info, length)

    fun deriveSessionKeyFromParams(
        pubParam: AsymmetricKeyParameter,
        prParam: AsymmetricKeyParameter,
        salt: ByteArray?,
        info: ByteArray? = null,
        length: Int = 32
    ): ByteArray {
        val sharedSecret = calculateSharedSecret(pubParam, prParam)
        return deriveSessionKeyFromSharedSecret(sharedSecret, salt, info, length)
    }

    fun calculateSharedSecret(
        pubParam: AsymmetricKeyParameter,
        prParam: AsymmetricKeyParameter
    ): ByteArray {
        log.d("Calculating shared secret")

        val agree = ECDHBasicAgreement()
        agree.init(prParam)
        val z = agree.calculateAgreement(pubParam)

        val result = BigIntegers.asUnsignedByteArray(agree.fieldSize, z)

        return result
    }

    fun deriveSessionKeyFromSharedSecret(
        sharedSecret: ByteArray,
        salt: ByteArray?,
        info: ByteArray? = null,
        length: Int = 32
    ): ByteArray {
        val hkdfParams = HKDFParameters(sharedSecret, salt, info)
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(hkdfParams)
        val data = ByteArray(length)
        hkdf.generateBytes(data, 0, data.size)

        return data
    }

    fun digest(input: ByteArray, algorithm: String): ByteArray? {
        val md: MessageDigest = MessageDigest.getInstance(algorithm)
        return md.digest(input)
    }

    fun digestSHA256(input: ByteArray): ByteArray? {
        val md: MessageDigest = MessageDigest.getInstance("SHA-256")
        return md.digest(input)
    }

    /**
     * Method for creating a signature for mdoc authentication
     * using Android KeyStore.
     * If ES256, ES384 or ES512 signature algorithms are used then resulting signature
     * is converted from DER encoding to R + S bytearray form.
     *
     * @param privateKey KeyStore private key which signs the data
     * @param payload Data which is to be signed
     * @param algorithm One of the supported algorithms specified in ISO 18013
     * @return signature
     */
    fun generateSignature(
        privateKey: PrivateKey,
        payload: ByteArray,
        algorithm: String
    ): ByteArray {
        val signing = Signature.getInstance(algorithm)
        signing.initSign(privateKey)
        signing.update(payload)
        val signature = signing.sign()

        // if ES256 or ES384 or ES512 algorithms are used for signature generation
        // then it needs to be converted from DER to concatenated R and S values
        // otherwise just return signature
        return when (algorithm) {
            ES256,
            ES384,
            ES512 -> convertDerToConcat(signature, algorithm)

            else -> signature
        }
    }

    fun generateRawSignature(
        signature: Signature,
        payload: ByteArray
    ): ByteArray {
        signature.update(payload)
        val signed = signature.sign()

        // if ES256 or ES384 or ES512 algorithms are used for signature generation
        // then it needs to be converted from DER to concatenated R and S values
        // otherwise just return signature
        return when (val algorithm = signature.algorithm) {
            ES256,
            ES384,
            ES512 -> convertDerToConcat(signed, algorithm)

            else -> signed
        }
    }

    /**
     * Extracts R and S component of DER encoded signature and adds them to a bytearray: R + S
     * This is needed for manually calculating signature for mdoc authentication
     *
     * @param der DER encoded signature bytearray
     * @param algorithm algorithm to use
     * @return bytearray with R and S components concatenated
     */
    private fun convertDerToConcat(der: ByteArray, algorithm: String): ByteArray {
        val len = when (algorithm) {
            ES256 -> 32
            ES384 -> 48
            ES512 -> 66
            else -> 32
        }

        // this is far too naive
        val concat = ByteArray(len * 2)

        // assumes SEQUENCE is organized as "R + S"
        var kLen = 4
        if (der[0].toInt() != 0x30) {
            throw CoseException("Unexpected signature input")
        }
        if (der[1].toInt() and 0x80 != 0) {
            // offset actually 4 + (7-bits of byte 1)
            kLen = 4 + (der[1] and 0x7f)
        }

        // calculate start/end of R
        var rOff = kLen
        var rLen: Int = der[rOff - 1].toInt()
        var rPad = 0
        if (rLen > len) {
            rOff += rLen - len
            rLen = len
        } else {
            rPad = len - rLen
        }

        // copy R
        System.arraycopy(der, rOff, concat, rPad, rLen)

        // calculate start/end of S
        var sOff = rOff + rLen + 2
        var sLen: Int = der[sOff - 1].toInt()
        var sPad = 0
        if (sLen > len) {
            sOff += sLen - len
            sLen = len
        } else {
            sPad = len - sLen
        }
        // copy S
        // copy S
        System.arraycopy(der, sOff, concat, len + sPad, sLen)

        return concat
    }
}
