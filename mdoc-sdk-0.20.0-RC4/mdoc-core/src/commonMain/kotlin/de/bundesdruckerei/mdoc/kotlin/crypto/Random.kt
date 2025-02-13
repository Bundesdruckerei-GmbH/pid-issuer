/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.crypto

import java.security.SecureRandom

val DEFAULT_SECURE_RANDOM: SecureRandom = try {
    SecureRandom.getInstanceStrong()
} catch (_: Throwable) {
    SecureRandom()
}
