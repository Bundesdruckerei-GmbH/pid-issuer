/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.vci.service.endpoints

import de.bdr.openid4vc.common.vci.NonceResponse
import de.bdr.openid4vc.common.vci.NonceService
import de.bdr.openid4vc.common.vci.NonceService.StandardNoncePurpose.C_NONCE
import de.bdr.openid4vc.vci.service.HttpHeaders
import de.bdr.openid4vc.vci.service.HttpRequest
import de.bdr.openid4vc.vci.service.HttpResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NonceEndpointService(val configuration: Configuration) {

    interface Configuration {
        val nonceEndpoint: String?
        val nonceService: NonceService
        val json: Json
    }

    private val nonceService = configuration.nonceService

    fun nonceEndpoint(httpRequest: HttpRequest<Unit>): HttpResponse {
        if (configuration.nonceEndpoint == null) {
            return HttpResponse(404, HttpHeaders.empty(), null)
        }
        val (nonce, validity) = nonceService.generate(C_NONCE)
        return HttpResponse(
            200,
            HttpHeaders.of(mapOf("cache-control" to "no-store")),
            configuration.json.encodeToString(NonceResponse(nonce, validity)),
        )
    }
}
