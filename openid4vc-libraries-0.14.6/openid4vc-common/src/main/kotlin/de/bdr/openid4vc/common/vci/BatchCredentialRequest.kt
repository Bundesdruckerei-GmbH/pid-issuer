/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BatchCredentialRequest(
    @SerialName("credential_requests") val credentialRequests: List<CredentialRequest>,
    @SerialName("credential_response_encryption")
    val credentialEncryption: CredentialEncryption? = null
)
