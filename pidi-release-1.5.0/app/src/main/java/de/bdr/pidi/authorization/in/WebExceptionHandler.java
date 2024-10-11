/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.in;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;
import de.bdr.openid4vc.vci.data.TokenType;
import de.bdr.pidi.authorization.core.exception.FinishAuthException;
import de.bdr.pidi.authorization.core.exception.OIDException;
import de.bdr.pidi.authorization.core.exception.ParameterTooLongException;
import de.bdr.pidi.authorization.core.exception.SessionNotFoundException;
import de.bdr.pidi.authorization.core.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.util.UriComponentsBuilder;

import static java.util.Optional.ofNullable;

@Slf4j
@ControllerAdvice
public class WebExceptionHandler {

    private static final String INTERNAL_SERVER_ERROR_MSG = "Internal server error";
    private static final String DPOP_SIGNING_ALGS =
            JWSAlgorithm.Family.SIGNATURE.stream().map(Algorithm::toJSONString).toList().toString();

    private final ObjectMapper mapper;

    public WebExceptionHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class, MissingServletRequestParameterException.class, ServletRequestBindingException.class, TypeMismatchException.class, HttpMessageNotReadableException.class, MethodArgumentNotValidException.class, MissingServletRequestPartException.class, BindException.class})
    public ResponseEntity<JsonNode> handleSpringClientException(Exception ex) {
        log.error("Spring could not process request", ex);
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Bad request");
    }

    @ExceptionHandler({MissingPathVariableException.class, ConversionNotSupportedException.class, HttpMessageNotWritableException.class, AsyncRequestTimeoutException.class})
    public ResponseEntity<JsonNode> handleSpringInternalException(Exception ex) {
        log.error("Spring could not process request", ex);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_MSG);
    }

    @ExceptionHandler(ParameterTooLongException.class)
    public ResponseEntity<JsonNode> handleParameterTooLongException(ParameterTooLongException ex) {
        log.error("Application Exception {}", ex.getLogMessage(), ex);
        return createErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(OIDException.class)
    public ResponseEntity<JsonNode> handleOidException(OIDException oe) {
        log.error("Application Exception {}", oe.getLogMessage(), oe);
        var headers = new LinkedMultiValueMap<String, String>();
        headers.setAll(oe.getHeader());
        return createErrorResponse(HttpStatus.BAD_REQUEST, oe.getErrorCode(), oe.getMessage(), headers);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorizedException(UnauthorizedException ue) {
        var scheme = ue.getScheme();
        log.error("UnauthorizedException scheme {}, error {}", scheme, ue.getError(), ue);
        var msg = new StringBuilder(scheme).append(" realm=\"oid4vci\"");
        if (ue.getError() != null && !ue.getError().isBlank()) {
            msg.append(", error=\"").append(ue.getError()).append("\"");
        }
        if (ue.getMessage() != null && !ue.getMessage().isBlank()) {
            msg.append(", error_description=\"").append(ue.getMessage()).append("\"");
        }
        if (TokenType.DPOP.getValue().equalsIgnoreCase(scheme)) {
            msg.append(", algs=").append(DPOP_SIGNING_ALGS);
        }

        var responseBuilder = ResponseEntity
                .status(HttpStatus.UNAUTHORIZED.value())
                .header(HttpHeaders.WWW_AUTHENTICATE, msg.toString());
        if (ue.getCause() instanceof OIDException ne) {
            ne.getHeader().forEach(responseBuilder::header);
        }
        return responseBuilder.build();
    }

    @ExceptionHandler(FinishAuthException.class)
    public ResponseEntity<Object> handleFinishAuthException(FinishAuthException ex) {
        var givenUri = ofNullable(ex.getRedirectUri()).orElse("http://localhost:8080/");
        var redirectBuilder = UriComponentsBuilder.fromHttpUrl(givenUri);
        String error;
        if (ex.getCause() instanceof OIDException oe) {
            if (oe.getMessage() != null) {
                redirectBuilder.queryParam("error_description", oe.getMessage());
            }
            error = oe.getErrorCode();
            log.warn("application exception caught: {}, {}", error, oe.getLogMessage(), oe);
        } else {
            error = "server_error";
            log.warn("application exception caught: {}", error, ex.getCause());
        }
        redirectBuilder.queryParam("error", error);
        if (ex.getState() != null) {
            redirectBuilder.queryParam("state", ex.getState());
        }
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectBuilder.toUriString())
                .build();
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<JsonNode> handleSessionNotFoundException(SessionNotFoundException ex) {
        log.error("Session was not found", ex);
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Bad request");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<JsonNode> handleRuntimeException(Exception ex) {
        log.error("Runtime Exception without further specification", ex);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_MSG);
    }

    private ResponseEntity<JsonNode> createErrorResponse(HttpStatus status, String message) {
        return createErrorResponse(status.value(), message, null, null);
    }

    private ResponseEntity<JsonNode> createErrorResponse(HttpStatus status, String message, String description) {
        return createErrorResponse(status.value(), message, description, null);
    }

    private ResponseEntity<JsonNode> createErrorResponse(HttpStatus status, String message, String description, MultiValueMap<String, String> headerValues) {
        return createErrorResponse(status.value(), message, description, headerValues);
    }

    private ResponseEntity<JsonNode> createErrorResponse(int statusCode, String message, String description, MultiValueMap<String, String> headerValues) {
        var headers = headerValues == null ? new HttpHeaders() : new HttpHeaders(headerValues);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ObjectNode body = mapper.createObjectNode()
                .put("error", message);
        if (description != null) {
            body.put("error_description", description);
        }
        return new ResponseEntity<>(body, headers, statusCode);
    }
}
