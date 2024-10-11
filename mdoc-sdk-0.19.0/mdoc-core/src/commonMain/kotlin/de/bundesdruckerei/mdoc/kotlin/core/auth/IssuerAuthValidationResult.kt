/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import de.bundesdruckerei.mdoc.kotlin.core.NameSpace
import de.bundesdruckerei.mdoc.kotlin.core.bstr
import de.bundesdruckerei.mdoc.kotlin.core.common.ValidationResult

class IssuerAuthValidationResult(
    val hasValidCertificatePath: Boolean,
    val hasValidSignature: Boolean,
    val hasValidDigests: Boolean,
    val invalidDigests: Map<NameSpace, List<Pair<IssuerSignedItem, bstr?>>>,
    val hasValidDocType: Boolean,
    val hasValidValidityInfo: Boolean
) : ValidationResult {
    override fun isValid(): Boolean {
        return hasValidCertificatePath
                && hasValidSignature && hasValidDigests
                && hasValidDocType && hasValidValidityInfo
    }
}
