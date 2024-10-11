/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CredentialResponse(
    @SerialName("credential") val credential: String? = null,
    @SerialName("credentials") val credentials: List<String> = emptyList(),
    @SerialName("transaction_id") val transactionId: String? = null,
    @SerialName("c_nonce") val credentialNonce: String? = null,
    @SerialName("c_nonce_expires_in") val credentialNonceExpiresIn: Int? = null,
    @SerialName("notification_id") val notificationId: String? = null,
) {
    init {
        require(
            listOf(credentials.isNotEmpty(), credential != null, transactionId != null).count {
                it
            } == 1
        ) {
            "Either credential, credentials or transaction id must be set"
        }
    }
}
