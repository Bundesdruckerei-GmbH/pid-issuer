/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.utils

import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.Ed25519Verifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyType
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64

/** The clock used by the library whenever the current instant, date or time is needed. */
var clock: Clock = Clock.systemUTC()

private val DEFAULT_RANDOM_CHARS =
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray()

private val random = SecureRandom()

private val rootPrefix = Regex("^\\$\\.?")

internal fun randomBase64(size: Int = 16): String {
    val bytes = ByteArray(size)
    random.nextBytes(bytes)
    return Base64.getEncoder().encodeToString(bytes)
}

internal fun randomString(size: Int = 22, chars: CharArray = DEFAULT_RANDOM_CHARS): String {
    val result = CharArray(size)
    for (i in 0 until size) {
        result[i] = chars[random.nextInt(chars.size)]
    }
    return String(result)
}

fun removeRootPathPrefix(path: String) = path.replace(rootPrefix, "")

fun jwsVerifierForKey(jwk: JWK): JWSVerifier =
    when (jwk.keyType) {
        KeyType.OKP -> {
            Ed25519Verifier(jwk.toOctetKeyPair())
        }
        KeyType.RSA -> {
            RSASSAVerifier(jwk.toRSAKey())
        }
        KeyType.EC -> {
            ECDSAVerifier(jwk.toECKey())
        }
        else -> {
            throw NotImplementedError("JWK signing algorithm not implemented")
        }
    }
