/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.response

import de.bundesdruckerei.mdoc.kotlin.core.uint

enum class ResponseStatus(val value: uint) {
    OK(0),
    GENERAL_ERROR(10),
    CBOR_DECODING_ERROR(11),
    CBOR_VALIDATION_ERROR(12);

    companion object {
        fun valueOf(value: uint): ResponseStatus =
            values().find { it.value == value } ?: GENERAL_ERROR
    }
}
