/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CredentialRequestError(
    @SerialName("error") val error: String,
    @SerialName("error_description") val errorDescription: String? = null,
    @SerialName("c_nonce") val credentialNonce: String? = null,
    @SerialName("c_nonce_expires_in") val credentialNonceExpiresIn: Int? = null
) {

    init {
        if (credentialNonceExpiresIn != null)
            require(credentialNonce != null) {
                "credentialNonce must be set if credentialNonceExpiresIn is set"
            }
    }

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
