/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.exceptions

import java.lang.IllegalArgumentException

/**
 * An `IllegalArgumentException` used during parsing and object construction to transport specific
 * error scenarios.
 */
class SpecificIllegalArgumentException(
    val reason: ReasonCode,
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause) {

    enum class ReasonCode {
        /**
         * Used when a proof is parsed or constructed with invalid proof_type or otherwise invalid
         * data (apart from validation happening when calling the validate methods).
         */
        INVALID_PROOF,

        /** Used when an unsupported credential format is used in a credential request. */
        INVALID_CREDENTIAL_FORMAT,

        /** Used when a claim value in a credential is invalid */
        INVALID_CLAIM,

        /** Used when an expected claim in a credential is missing */
        MISSING_CLAIM,
    }
}
