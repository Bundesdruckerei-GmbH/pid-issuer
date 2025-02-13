/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci

import de.bdr.openid4vc.common.clock
import de.bdr.openid4vc.common.vci.NonceService.NonceAndValidityDuration
import de.bdr.openid4vc.common.vci.NonceService.NoncePurpose
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Cipher.DECRYPT_MODE
import javax.crypto.Cipher.ENCRYPT_MODE
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptedNonceService(key: ByteArray, private val maxNonceAge: Duration) : NonceService {

    private val random = SecureRandom()

    private val key = SecretKeySpec(key, "AES")

    override fun generate(purpose: NoncePurpose): NonceAndValidityDuration {
        val purposeBytes = purpose.bytes
        val dateBytes = Instant.now(clock).toEpochMilli().toBigInteger().toByteArray()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        random.nextBytes(iv)
        cipher.init(ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.update(purposeBytes)
        val padBytes = ByteArray(8 - dateBytes.size)
        cipher.update(padBytes)
        val ciphertext = cipher.doFinal(dateBytes)
        val buffer = ByteBuffer.wrap(ByteArray(12 + ciphertext.size))
        buffer.put(iv)
        buffer.put(ciphertext)
        return NonceAndValidityDuration(
            Base64.getEncoder().encodeToString(buffer.array()),
            maxNonceAge,
        )
    }

    override fun validate(nonce: String, expectedPurpose: NoncePurpose) {
        try {
            val decoded = Base64.getDecoder().decode(nonce)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(DECRYPT_MODE, key, GCMParameterSpec(128, decoded, 0, 12))
            val cleartext = ByteBuffer.wrap(cipher.doFinal(decoded, 12, decoded.size - 12))

            check(cleartext.capacity() == 8 + expectedPurpose.bytes.size) {
                "Invalid data in decrypted nonce"
            }

            val purposeBytes = ByteArray(expectedPurpose.bytes.size)
            val dateBytes = ByteArray(8)

            cleartext[purposeBytes]
            cleartext[dateBytes]

            require(purposeBytes.contentEquals(expectedPurpose.bytes)) { "Purpose mismatch" }

            val creation = Instant.ofEpochMilli(BigInteger(dateBytes).toLong())
            val now = Instant.now(clock)
            require(creation.plus(maxNonceAge).isAfter(now)) { "Nonce is too old" }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid nonce", e)
        }
    }
}
