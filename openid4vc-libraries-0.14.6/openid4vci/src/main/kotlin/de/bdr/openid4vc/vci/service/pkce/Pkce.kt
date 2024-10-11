/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.service.pkce

import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

object Pkce {
    private val rng: SecureRandom = SecureRandom()

    /** Creates a code-verifier string according to RFC 7636 section 4.1 */
    fun codeVerifier(): String {
        val bytes = ByteArray(32)
        rng.nextBytes(bytes)

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun validate(
        codeChallenge: String,
        codeVerifier: String,
        codeChallengeMethod: PkceCodeChallengeMethod = PkceCodeChallengeMethod.PLAIN
    ): Boolean {
        return if (codeChallengeMethod == PkceCodeChallengeMethod.PLAIN) {
            codeVerifier == codeChallenge
        } else {
            codeChallenge == codeChallenge(codeVerifier)
        }
    }

    fun codeChallenge(codeVerifier: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encode(md.digest(codeVerifier.toByteArray()))
            .toString(Charset.defaultCharset())
    }
}
