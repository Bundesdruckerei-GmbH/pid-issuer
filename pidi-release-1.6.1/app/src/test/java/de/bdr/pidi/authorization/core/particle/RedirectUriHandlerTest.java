/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidGrantException;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.exception.ValidationFailedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectUriHandlerTest {
    private static final String REDIRECT_URI_PARAM_NAME = "redirect_uri";
    private static final String REDIRECT_URI = "https://secure.redirect.com";

    private final RedirectUriHandler redirectUriHandlerUT = new RedirectUriHandler();
    @Mock
    WSession session;

    public static Stream<Arguments> redirectUriProvider() {
        return Stream.of(
                Arguments.arguments(null, "redirect_uri must not be empty"),
                Arguments.arguments("", "redirect_uri must not be empty"),
                Arguments.arguments("https   invalid uri", "redirect_uri must be a valid URI"),
                Arguments.arguments("http://unsecure.redirect.com", "redirect_uri must start with https"));
    }

    @DisplayName("Verify exception when missing redirect_uri parameter")
    @Test
    void test001() {
        HttpRequest<?> request = RequestUtil.getHttpRequest(new HashMap<>());
        WResponseBuilder responseBuilder = new WResponseBuilder();

        var exception = assertThrows(InvalidRequestException.class, () ->
                redirectUriHandlerUT.processPushedAuthRequest(request, responseBuilder, session));

        assertAll(
                () -> assertThat(exception.getMessage(), is("Missing required parameter 'redirect_uri'")),
                () -> assertThat(exception.getLogMessage(), is("Missing required parameter 'redirect_uri'")),
                () -> verify(session, never()).putParameter(eq(SessionKey.REDIRECT_URI), anyString())

        );
    }

    @DisplayName("Verify exception when invalid value redirect_uri parameter")
    @ParameterizedTest
    @MethodSource("redirectUriProvider")
    void test002(String redirectUri, String expectedLogMessage) {
        var parameter = new HashMap<String, String>();
        parameter.put(REDIRECT_URI_PARAM_NAME, redirectUri);
        HttpRequest<?> request = RequestUtil.getHttpRequest(parameter);
        WResponseBuilder responseBuilder = new WResponseBuilder();

        var exception = assertThrows(ValidationFailedException.class, () ->
                redirectUriHandlerUT.processPushedAuthRequest(request, responseBuilder, session));

        assertAll(
                () -> assertThat(exception.getMessage(), is("Invalid redirect URI")),
                () -> assertThat(exception.getLogMessage(), is(expectedLogMessage)),
                () -> verify(session, never()).putParameter(eq(SessionKey.REDIRECT_URI), anyString())
        );
    }

    @DisplayName("Verify https uri redirect_uri parameter")
    @Test
    void test006() {
        HttpRequest<?> request = RequestUtil.getHttpRequest(Map.of(REDIRECT_URI_PARAM_NAME, REDIRECT_URI));
        WResponseBuilder responseBuilder = new WResponseBuilder();

        redirectUriHandlerUT.processPushedAuthRequest(request, responseBuilder, session);

        verify(session, times(1)).putParameter(SessionKey.REDIRECT_URI, REDIRECT_URI);
    }

    @DisplayName("Verify https uri redirect_uri parameter on token request")
    @Test
    void test007() {
        HttpRequest<?> request = RequestUtil.getHttpRequest(Map.of(REDIRECT_URI_PARAM_NAME, REDIRECT_URI));
        WResponseBuilder responseBuilder = new WResponseBuilder();

        when(session.getParameter(SessionKey.REDIRECT_URI)).thenReturn(REDIRECT_URI);
        assertDoesNotThrow(() -> redirectUriHandlerUT.processTokenRequest(request, responseBuilder, session));
    }

    @DisplayName("Verify invalid redirect_uri parameter on token request")
    @Test
    void test008() {
        HttpRequest<?> request = RequestUtil.getHttpRequest(Map.of(REDIRECT_URI_PARAM_NAME, "https://other.redirect.com"));
        WResponseBuilder responseBuilder = new WResponseBuilder();

        when(session.getParameter(SessionKey.REDIRECT_URI)).thenReturn(REDIRECT_URI);
        assertThrows(InvalidGrantException.class, () -> redirectUriHandlerUT.processTokenRequest(request, responseBuilder, session));
    }
}
