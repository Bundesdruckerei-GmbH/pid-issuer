/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.vci.service.endpoints

import de.bdr.openid4vc.common.vci.CredentialRequestError
import de.bdr.openid4vc.vci.service.CONTENT_TYPE_APPLICATION_JSON
import de.bdr.openid4vc.vci.service.HttpHeaders
import de.bdr.openid4vc.vci.service.HttpResponse
import de.bdr.openid4vc.vci.service.HttpResponseException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class IllegalCredentialRequestException(
    val reason: Reason,
    val errorDescription: String? = null,
    val nonce: String? = null,
    val expiresIn: Int? = null,
) :
    HttpResponseException(
        HttpResponse(
            reason.status,
            HttpHeaders(CONTENT_TYPE_APPLICATION_JSON),
            Json.encodeToString(
                CredentialRequestError(error = reason.error, errorDescription = errorDescription)
            ),
        )
    ) {

    enum class Reason(val status: Int, val error: String) {
        INVALID_ACCESS_TOKEN(403, CredentialRequestError.INVALID_TOKEN),
        INVALID_FORMAT(400, CredentialRequestError.UNSUPPORTED_CREDENTIAL_FORMAT),
        INVALID_PROOF(400, CredentialRequestError.INVALID_PROOF),
        FAILED_TO_PARSE(400, CredentialRequestError.INVALID_CREDENTIAL_REQUEST),
        INVALID_REQUEST(400, CredentialRequestError.INVALID_CREDENTIAL_REQUEST),
        PROOF_PRESENT(400, CredentialRequestError.INVALID_CREDENTIAL_REQUEST),
        INVALID_CREDENTIAL_ENCRYPTION_PARAMETERS(
            400,
            CredentialRequestError.INVALID_ENCRYPTION_PARAMETERS,
        ),
    }

    override fun toString() = reason.name
}
