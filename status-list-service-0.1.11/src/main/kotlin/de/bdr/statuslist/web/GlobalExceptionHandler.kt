/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.web

import de.bdr.statuslist.util.log
import de.bdr.statuslist.web.api.model.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ErrorResponseException::class)
    fun handleErrorResponseException(e: ErrorResponseException): ResponseEntity<ErrorResponse> {
        log.error(e.response.message, e)
        return ResponseEntity.status(e.status).body(e.response)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        e: HttpMessageNotReadableException
    ): ResponseEntity<ErrorResponse> {
        log.error(e.message, e)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("BAD_REQUEST", e.message ?: "HTTP message not readable"))
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(e: RuntimeException): ResponseEntity<ErrorResponse> {
        log.error(e.message, e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    "INTERNAL_SERVER_ERROR",
                    e.message ?: HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
                )
            )
    }
}

class ErrorResponseException(val status: HttpStatus, val response: ErrorResponse) :
    RuntimeException()

enum class ErrorCode {
    UNAUTHORIZED,
    NO_SUCH_POOL,
    NO_SUCH_LIST,
    VALUE_OUT_OF_RANGE,
    INDEX_OUT_OF_BOUNDS,
    RATE_LIMIT_REACHED,
    UNSUPPORTED_MEDIA_TYPE,
}
