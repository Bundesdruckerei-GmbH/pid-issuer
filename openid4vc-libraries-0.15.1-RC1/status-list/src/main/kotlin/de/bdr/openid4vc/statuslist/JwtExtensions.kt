/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.statuslist

import com.nimbusds.jwt.EncryptedJWT
import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.PlainJWT
import com.nimbusds.jwt.SignedJWT
import java.text.ParseException

internal fun parseJwt(s: String): JWT {
    return try {
        SignedJWT.parse(s)
    } catch (e1: Exception) {
        try {
            PlainJWT.parse(s)
        } catch (e2: Exception) {
            try {
                EncryptedJWT.parse(s)
            } catch (e3: Exception) {
                val e = ParseException("Failed to parse value as signed, plain or encrypted JWT", 0)
                e.initCause(e1)
                e.addSuppressed(e2)
                e.addSuppressed(e3)
                throw e
            }
        }
    }
}
