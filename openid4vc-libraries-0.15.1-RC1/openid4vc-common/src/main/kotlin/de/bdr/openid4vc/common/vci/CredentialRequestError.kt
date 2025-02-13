/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CredentialRequestError(
    @SerialName("error") val error: String,
    @SerialName("error_description") val errorDescription: String? = null,
) {

    companion object {
        // Definied in RFC6750
        const val INVALID_TOKEN = "invalid_token"
        // Following defined in OID4VCI
        const val INVALID_CREDENTIAL_REQUEST = "invalid_credential_request"
        const val UNSUPPORTED_CREDENTIAL_TYPE = "unsupported_credential_type"
        const val UNSUPPORTED_CREDENTIAL_FORMAT = "unsupported_credential_format"
        const val INVALID_PROOF = "invalid_proof"
        const val INVALID_ENCRYPTION_PARAMETERS = "invalid_encryption_parameters"
    }
}
