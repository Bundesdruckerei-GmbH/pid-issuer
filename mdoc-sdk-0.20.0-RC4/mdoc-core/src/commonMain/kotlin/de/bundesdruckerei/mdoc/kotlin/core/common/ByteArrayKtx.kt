/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.common

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte ->
    "%02x".format(eachByte)
}
