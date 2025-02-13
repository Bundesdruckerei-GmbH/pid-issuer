/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
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
