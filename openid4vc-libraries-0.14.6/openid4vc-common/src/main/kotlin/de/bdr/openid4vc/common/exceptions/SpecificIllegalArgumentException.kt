/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
    cause: Throwable? = null
) : IllegalArgumentException(message, cause) {

    enum class ReasonCode {
        /**
         * Used when a proof is parsed or constructed with invalid proof_type or otherwise invalid
         * data (apart from validation happening when calling the validate methods).
         */
        INVALID_PROOF,

        /** Used when an unsupported credential format is used in a credential request. */
        INVALID_CREDENTIAL_FORMAT
    }
}
