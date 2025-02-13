/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.web

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ErrorController : ErrorController {

    @GetMapping("/error")
    fun error(request: HttpServletRequest, response: HttpServletResponse) {
        val status = request.getErrorStatus()
        response.status = status.value()
        response.writer.write(status.reasonPhrase)
    }

    private fun HttpServletRequest.getErrorStatus(): HttpStatus {
        val statusCode = getAttribute(RequestDispatcher.ERROR_STATUS_CODE) as Int? ?: 500
        return try {
            HttpStatus.valueOf(statusCode)
        } catch (ex: Exception) {
            HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}
