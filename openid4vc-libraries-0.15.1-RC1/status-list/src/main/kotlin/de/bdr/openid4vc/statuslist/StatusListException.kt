/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.statuslist

class StatusListException(val reason: Reason, message: String? = null, cause: Throwable? = null) :
    Exception(message, cause) {

    enum class Reason {
        INVALID_TYPE,
        UNSUPPORTED_ALGORITHM,
        MISSING_CLAIMS,
        INVALID_TIME,
        EXPIRED,
        INVALID_CLAIM,
        INVALID_SIGNATURE,
        INVALID_STATUS_LIST,
        INVALID_STATUS_REFERENCE,
        INVALID_STATUS_LIST_AGGREGATION
    }

    override fun toString() = reason.name
}

internal fun fail(
    error: StatusListException.Reason,
    message: String? = null,
    cause: Throwable? = null
): Nothing {
    throw StatusListException(error, message, cause)
}
