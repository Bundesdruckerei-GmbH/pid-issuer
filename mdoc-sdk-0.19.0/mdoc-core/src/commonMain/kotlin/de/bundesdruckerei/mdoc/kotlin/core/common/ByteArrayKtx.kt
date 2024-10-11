/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.common

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte ->
    "%02x".format(eachByte)
}
