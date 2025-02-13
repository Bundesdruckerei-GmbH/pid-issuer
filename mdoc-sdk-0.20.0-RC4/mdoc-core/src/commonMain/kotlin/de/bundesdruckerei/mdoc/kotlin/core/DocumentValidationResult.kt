/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core

import de.bundesdruckerei.mdoc.kotlin.core.auth.IssuerAuthValidationResult
import de.bundesdruckerei.mdoc.kotlin.core.common.ValidationResult
import de.bundesdruckerei.mdoc.kotlin.core.deviceauth.DeviceAuthValidationResult

open class DocumentValidationResult(
    val issuerAuthValidationResult: IssuerAuthValidationResult,
    val deviceAuthValidationResult: DeviceAuthValidationResult,
) : ValidationResult {
    override fun isValid(): Boolean {
        return issuerAuthValidationResult.isValid() && deviceAuthValidationResult.isValid()
    }
}
