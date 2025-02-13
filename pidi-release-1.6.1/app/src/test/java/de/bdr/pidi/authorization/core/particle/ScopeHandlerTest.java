/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.OIDException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScopeHandlerTest {
    public static Stream<Arguments> validScopesProvider() {
        return Stream.of(
                arguments(FlowVariant.C, "pid"),
                arguments(FlowVariant.C1, "pid")
        );
    }

    public static Stream<Arguments> invalidScopesProvider() {
        return Stream.of(
                arguments(FlowVariant.C, "pidsdjwt"),
                arguments(FlowVariant.C1, "")
        );
    }

    @DisplayName("Validate valid scopes on PAR")
    @ParameterizedTest
    @MethodSource("validScopesProvider")
    void validateValidScopes(FlowVariant flowVariant, String scope) {
        ScopeHandler scopeHandlerUT = new ScopeHandler(flowVariant);
        WSession sessionMock = mock(WSession.class);
        HttpRequest<?> requestMock = mock(HttpRequest.class);
        when(requestMock.getParameters()).thenReturn(Map.of("scope", scope));
        assertDoesNotThrow(() -> scopeHandlerUT.processPushedAuthRequest(requestMock, new WResponseBuilder(), sessionMock));
        verify(sessionMock).putParameter(SessionKey.SCOPE, scope);
    }

    @DisplayName("Validate invalid scopes")
    @ParameterizedTest
    @MethodSource("invalidScopesProvider")
    void validateInvalidScopes(FlowVariant flowVariant, String scope) {
        ScopeHandler scopeHandlerUT = new ScopeHandler(flowVariant);
        WSession sessionMock = mock(WSession.class);
        HttpRequest<?> requestMock = mock(HttpRequest.class);
        when(requestMock.getParameters()).thenReturn(Map.of("scope", scope));
        WResponseBuilder responseBuilder = new WResponseBuilder();
        assertThrows(OIDException.class,
                () -> scopeHandlerUT.processPushedAuthRequest(requestMock, responseBuilder, sessionMock));
        assertThrows(OIDException.class,
                () -> scopeHandlerUT.processRefreshTokenRequest(requestMock, responseBuilder, sessionMock));
        verify(sessionMock, never()).putParameter(SessionKey.SCOPE, scope);
    }

    @DisplayName("Validate valid scopes on refresh token request")
    @ParameterizedTest
    @ValueSource(strings = {"pid"})
    @NullAndEmptySource
    void validateValidScopesOnRefreshTokenRequest(String scope) {
        ScopeHandler scopeHandlerUT = new ScopeHandler(FlowVariant.C1);
        WSession sessionMock = mock(WSession.class);
        HttpRequest<?> requestMock = mock(HttpRequest.class);
        if (scope != null && !scope.isEmpty()) {
            when(requestMock.getParameters()).thenReturn(Map.of("scope", scope));
        }
        assertDoesNotThrow(() -> scopeHandlerUT.processRefreshTokenRequest(requestMock, new WResponseBuilder(), sessionMock));
        if (scope != null && !scope.isEmpty()) {
            verify(sessionMock).putParameter(SessionKey.SCOPE, scope);
        }
    }
}
