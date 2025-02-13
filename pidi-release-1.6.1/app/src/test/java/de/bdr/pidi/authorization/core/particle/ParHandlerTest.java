/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasLength;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ParHandlerTest {
    private final ParHandler parHandlerUT = new ParHandler(Duration.ofSeconds(60));

    @Mock
    private WSession session;

    @DisplayName("Verify exception when missing response_type parameter")
    @Test
    void test003() {
        HttpRequest<?> request = RequestUtil.getHttpRequest(new HashMap<>());
        WResponseBuilder responseBuilder = new WResponseBuilder();

        var exception = assertThrows(InvalidRequestException.class, () ->
                parHandlerUT.processPushedAuthRequest(request, responseBuilder, session));

        assertAll(
                () -> assertThat(exception.getMessage(), is("Missing required parameter 'response_type'")),
                () -> assertThat(exception.getLogMessage(), is("Missing required parameter 'response_type'"))
        );
    }

    @DisplayName("Verify exception when null response_type parameter")
    @Test
    void test004() {
        var parameter = new HashMap<String, String>();
        parameter.put("response_type", null);
        HttpRequest<?> request = RequestUtil.getHttpRequest(parameter);
        WResponseBuilder responseBuilder = new WResponseBuilder();

        var exception = assertThrows(InvalidRequestException.class, () ->
                parHandlerUT.processPushedAuthRequest(request, responseBuilder, session));

        assertAll(
                () -> assertThat(exception.getMessage(), is("Invalid response type")),
                () -> assertThat(exception.getLogMessage(), is("response_type must not be empty"))
        );
    }

    @DisplayName("Verify exception when empty response_type parameter")
    @Test
    void test005() {
        HttpRequest<?> request = RequestUtil.getHttpRequest(Map.of("response_type", ""));
        WResponseBuilder responseBuilder = new WResponseBuilder();

        var exception = assertThrows(InvalidRequestException.class, () ->
                parHandlerUT.processPushedAuthRequest(request, responseBuilder, session));

        assertAll(
                () -> assertThat(exception.getMessage(), is("Invalid response type")),
                () -> assertThat(exception.getLogMessage(), is("response_type must not be empty"))
        );
    }

    @DisplayName("Verify exception when invalid response_type parameter")
    @Test
    void test006() {
        HttpRequest<?> request = RequestUtil.getHttpRequest(Map.of("response_type", "invalid"));
        WResponseBuilder responseBuilder = new WResponseBuilder();

        var exception = assertThrows(UnsupportedResponseTypeException.class, () ->
                parHandlerUT.processPushedAuthRequest(request, responseBuilder, session));

        assertAll(
                () -> assertThat(exception.getMessage(), is("Unsupported response type: invalid")),
                () -> assertThat(exception.getLogMessage(), is("Unsupported response type: invalid"))
        );
    }

    @DisplayName("Verify store data in session when valid response_type parameter")
    @Test
    void test007() {
        HttpRequest<?> request = RequestUtil.getHttpRequest(Map.of("response_type", "code"));
        WResponseBuilder responseBuilder = new WResponseBuilder();

        assertDoesNotThrow(() -> parHandlerUT.processPushedAuthRequest(request, responseBuilder, session));

        verify(session).putParameter(eq(SessionKey.REQUEST_URI), anyString());
        verify(session).putParameter(eq(SessionKey.REQUEST_URI_EXP_TIME), any(Instant.class));

        var response = responseBuilder.buildJSONResponseEntity();

        assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.CREATED)),
                () -> assertThat(response.getHeaders().get("Content-Type").get(0), is("application/json")),
                () -> assertThat(response.getBody().get("expires_in").asInt(), is(60)),
                () -> assertThat(response.getBody().get("request_uri").asText(), startsWith("urn:ietf:params:oauth:request_uri:")),
                () -> assertThat(response.getBody().get("request_uri").asText(), hasLength(56))
        );

    }
}
