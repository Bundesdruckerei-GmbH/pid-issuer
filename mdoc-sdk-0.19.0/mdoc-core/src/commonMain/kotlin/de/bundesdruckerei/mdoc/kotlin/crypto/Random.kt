/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.crypto

import java.security.SecureRandom

val DEFAULT_SECURE_RANDOM: SecureRandom = try {
    SecureRandom.getInstanceStrong()
} catch (_: Throwable) {
    SecureRandom()
}
