/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
