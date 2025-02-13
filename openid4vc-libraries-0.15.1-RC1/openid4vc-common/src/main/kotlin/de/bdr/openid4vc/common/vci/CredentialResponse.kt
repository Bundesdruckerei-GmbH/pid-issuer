/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CredentialResponse(
    @SerialName("credentials") val credentials: List<Credential> = emptyList(),
    @SerialName("transaction_id") val transactionId: String? = null,
    @SerialName("notification_id") val notificationId: String? = null,
) {
    init {
        require(listOf(credentials.isNotEmpty(), transactionId != null).count { it } == 1) {
            "Either credentials or transaction id must be set"
        }
    }
}

// TODO: make Credential class open to support credential specific data
@Serializable data class Credential(@SerialName("credential") val credential: String)
