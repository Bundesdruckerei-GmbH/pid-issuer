/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.web

import de.bdr.statuslist.config.AppConfiguration
import de.bdr.statuslist.config.StatusListPoolConfiguration
import de.bdr.statuslist.data.ValueOutOfRangeException
import de.bdr.statuslist.service.PrefetchBufferUnderflowException
import de.bdr.statuslist.service.StatusListService
import de.bdr.statuslist.util.sha256
import de.bdr.statuslist.web.api.DefaultInternalApi
import de.bdr.statuslist.web.api.model.ErrorResponse
import de.bdr.statuslist.web.api.model.Reference
import de.bdr.statuslist.web.api.model.References
import de.bdr.statuslist.web.api.model.UpdateStatusRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalApiController(
    private val config: AppConfiguration,
    private val service: StatusListService,
) : DefaultInternalApi {

    override fun newReferences(
        @PathVariable(value = "id") id: String,
        @Min(value = 1.toLong())
        @Valid
        @RequestParam(defaultValue = "1", required = false, value = "amount")
        amount: Int,
        @RequestHeader(required = false, value = "X-Api-Key") xApiKey: String?,
    ): ResponseEntity<References> {
        check(amount > 0) {
            throw ErrorResponseException(
                BAD_REQUEST,
                ErrorResponse(ErrorCode.VALUE_OUT_OF_RANGE.name, "Amount must be greater than 0"),
            )
        }
        val poolConfig = config(id)

        authenticate(poolConfig, xApiKey)

        try {
            return ResponseEntity.ok()
                .body(References(service.reserve(id, amount).map { Reference(it.uri, it.index) }))
        } catch (e: PrefetchBufferUnderflowException) {
            throw ErrorResponseException(
                TOO_MANY_REQUESTS,
                ErrorResponse(
                    ErrorCode.RATE_LIMIT_REACHED.name,
                    "Too many indices requested from pool $id",
                ),
            )
        }
    }

    override fun updateStatus(
        @Valid @RequestBody updateStatusRequest: UpdateStatusRequest,
        @RequestHeader(required = false, value = "X-Api-Key") xApiKey: String?,
    ): ResponseEntity<Unit> {
        try {
            val poolConfig = config(service.poolId(updateStatusRequest.uri))
            authenticate(poolConfig, xApiKey)
        } catch (e: IllegalStateException) {
            throw ErrorResponseException(
                BAD_REQUEST,
                ErrorResponse(ErrorCode.NO_SUCH_LIST.name, e.message ?: "No such list"),
            )
        }

        try {
            service.updateStatus(
                updateStatusRequest.uri,
                updateStatusRequest.index,
                updateStatusRequest.value,
            )
            return ResponseEntity.noContent().build()
        } catch (e: IndexOutOfBoundsException) {
            throw ErrorResponseException(
                BAD_REQUEST,
                ErrorResponse(
                    ErrorCode.INDEX_OUT_OF_BOUNDS.name,
                    "Index out of bounds (uri=${updateStatusRequest.uri}, index=${updateStatusRequest.index})",
                ),
            )
        } catch (e: ValueOutOfRangeException) {
            throw ErrorResponseException(
                BAD_REQUEST,
                ErrorResponse(
                    ErrorCode.VALUE_OUT_OF_RANGE.name,
                    "Value out of range (uri=${updateStatusRequest.uri}, value=${updateStatusRequest.value})",
                ),
            )
        }
    }

    private fun config(poolId: String) =
        config.statusListPools[poolId]
            ?: throw ErrorResponseException(
                NOT_FOUND,
                ErrorResponse(ErrorCode.NO_SUCH_POOL.name, "No pool with id $poolId"),
            )

    private fun authenticate(poolConfig: StatusListPoolConfiguration, apiKey: String?) {
        if (apiKey == null) {
            throw ErrorResponseException(
                UNAUTHORIZED,
                ErrorResponse(ErrorCode.UNAUTHORIZED.name, "Missing api key"),
            )
        }
        if (!poolConfig.apiKeyHashes.contains(sha256(apiKey.toByteArray()))) {
            throw ErrorResponseException(
                UNAUTHORIZED,
                ErrorResponse(ErrorCode.UNAUTHORIZED.name, "Invalid api key"),
            )
        }
    }
}
