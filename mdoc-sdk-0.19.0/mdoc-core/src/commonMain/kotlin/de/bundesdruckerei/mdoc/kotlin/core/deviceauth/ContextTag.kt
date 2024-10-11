/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import de.bundesdruckerei.mdoc.kotlin.core.common.InvalidContextTagException
import de.bundesdruckerei.mdoc.kotlin.core.tstr

enum class ContextTag(val tag: tstr) {

    Mac("deviceMac"),
    Sig("deviceSignature"),
    ;

    companion object {
        fun fromString(value: tstr): ContextTag =
            entries.firstOrNull { it.tag == value } ?: throw InvalidContextTagException(value)
    }
}
