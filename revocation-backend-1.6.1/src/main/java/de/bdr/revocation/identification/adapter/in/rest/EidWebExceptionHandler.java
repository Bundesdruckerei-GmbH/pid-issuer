/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.adapter.in.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bdr.revocation.identification.core.exception.AuthenticationNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@RequiredArgsConstructor
@ControllerAdvice("de.bdr.revocation.identification")
public class EidWebExceptionHandler {
    private static final String INTERNAL_SERVER_ERROR_MSG = "Internal server error";

    private final ObjectMapper mapper;

    @ExceptionHandler({AuthenticationNotFoundException.class})
    public ResponseEntity<JsonNode> handleAuthenticationNotFoundException(AuthenticationNotFoundException ex) {
        log.error("Authentication not found", ex);
        return createErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler({RuntimeException.class})
    public ResponseEntity<JsonNode> handleRuntimeException(RuntimeException ex) {
        log.error("Server could not process request", ex);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_MSG);
    }

    private ResponseEntity<JsonNode> createErrorResponse(HttpStatus status, String message) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        ObjectNode body = mapper.createObjectNode()
                .put("message", message);
        return new ResponseEntity<>(body, headers, status.value());
    }
}
