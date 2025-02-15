/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import de.bundesdruckerei.mdoc.kotlin.core.DataElementIdentifier
import de.bundesdruckerei.mdoc.kotlin.core.NameSpace
import de.bundesdruckerei.mdoc.kotlin.core.common.ValidationResult

class DeviceAuthValidationResult(
    val hasValidMac: Boolean? = null,
    val hasValidSignature: Boolean? = null,
    val unauthorizedKeyUsage: Map<NameSpace, List<DataElementIdentifier>>? = null,
) : ValidationResult {
    override fun isValid(): Boolean {
        return (hasValidMac == true || hasValidSignature == true) && unauthorizedKeyUsage?.isEmpty() ?: true
    }
}
