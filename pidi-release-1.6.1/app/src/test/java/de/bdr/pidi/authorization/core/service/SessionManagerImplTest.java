/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.service;

import de.bdr.openid4vc.vci.data.TokenType;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.AuthorizationHousekeeping;
import de.bdr.pidi.authorization.core.SessionManager;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.exception.SessionNotFoundException;
import de.bdr.pidi.authorization.core.exception.UnauthorizedException;
import de.bdr.pidi.authorization.out.persistence.PidiNonceAdapter;
import de.bdr.pidi.authorization.out.persistence.PidiSessionAdapter;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.bdr.pidi.authorization.ConfigTestData.AUTH_CONFIG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SessionManagerImplTest {
    private static final long VALID_SESSION_ID = 123456789L;
    private static final String TOKEN_TYPE = TokenType.DPOP.getValue();

    @Mock
    private PidiSessionAdapter sessionAdapter;
    @Mock
    private PidiNonceAdapter pidiNonceAdapter;
    @Spy
    private AuthorizationConfiguration config = AUTH_CONFIG;
    private SessionManager sessionManagerUT;

    @BeforeEach
    void setUp() {
        sessionManagerUT = new SessionManagerImpl(sessionAdapter, pidiNonceAdapter, config);
    }

    @DisplayName("Verify invalid request_uri parameter")
    @ParameterizedTest
    @ValueSource(strings = {"urn:ietf:params:oauth:request_uri:abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
            "urn:ietf:params:oauth:request_uri",
            "urn:ietf:params:oauth:request_uri.abcdefghijklmnopqrstuv",
            ""})
    void test001(String requestUri) {
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> sessionManagerUT.loadByRequestUri(requestUri, FlowVariant.C));

        assertAll(
                () -> assertThat(exception.getMessage(), is("invalid request_uri")),
                () -> assertThat(exception.getLogMessage(), is("Invalid request_uri: " + requestUri))
        );
    }

    @DisplayName("Verify null value request_uri")
    @Test
    void test002() {
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> sessionManagerUT.loadByRequestUri(null, FlowVariant.C));

        assertAll(
                () -> assertThat(exception.getMessage(), is("invalid request_uri")),
                () -> assertThat(exception.getLogMessage(), is("Invalid request_uri: null"))
        );
    }

    @DisplayName("Verify valid value request_uri")
    @Test
    void test003() {
        var requestUri = TestUtils.generateRequestUri();
        prepareMockInit();
        prepareMockRequestUri(requestUri);
        var session = sessionManagerUT.init(FlowVariant.C);
        session.putParameter(SessionKey.REQUEST_URI, requestUri);
        sessionManagerUT.persist(session);

        var found = sessionManagerUT.loadByRequestUri(requestUri, FlowVariant.C);
        assertNotNull(found);

        assertEquals(session, found);

        assertThat(session, is(not(sameInstance(found))));

        verify(sessionAdapter).update(any(), any());
    }

    @Test
    void when_init_then_return_session() {
        prepareMockInit();
        var result = sessionManagerUT.init(FlowVariant.C);

        assertNotNull(result);

        result.putParameter(SessionKey.SCOPE, "bar");
        assertEquals("bar", result.getParameter(SessionKey.SCOPE));
    }

    @Test
    void given_putParameter_when_loadByAuthorizationCode_then_ok() {
        var code = TestUtils.generateAuthorizationCode();
        prepareMockInit();
        prepareMockAuthorizationCode(code);
        var result = sessionManagerUT.init(FlowVariant.C);
        result.putParameter(SessionKey.AUTHORIZATION_CODE, code);

        var found = sessionManagerUT.loadByAuthorizationCode(code, FlowVariant.C);

        assertNotNull(found);

        assertEquals(result, found);

        assertThat(result, is(not(sameInstance(found))));
    }

    @DisplayName("Verify valid value issuer_state")
    @Test
    void test004() {
        var issuerState = TestUtils.generateIssuerState();
        prepareMockInit();
        prepareMockIssuerState(issuerState);
        var session = sessionManagerUT.init(FlowVariant.C);
        session.putParameter(SessionKey.ISSUER_STATE, issuerState);
        sessionManagerUT.persist(session);

        var found = sessionManagerUT.loadByIssuerState(issuerState, FlowVariant.C);
        assertNotNull(found);

        assertEquals(session, found);

        assertThat(session, is(not(sameInstance(found))));

        verify(sessionAdapter).update(any(), any());
    }

    @DisplayName("Verify null value issuer_state")
    @Test
    void test005() {
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> sessionManagerUT.loadByIssuerState(null, FlowVariant.C));

        assertAll(
                () -> assertThat(exception.getMessage(), is("invalid issuer_state")),
                () -> assertThat(exception.getLogMessage(), is("Invalid issuer_state: null"))
        );
    }

    @DisplayName("Verify no session with given issuer_state")
    @Test
    void test006() {
        doThrow(SessionNotFoundException.class).when(sessionAdapter).findByIssuerState(anyString());
        var issuerState = TestUtils.generateIssuerState();
        assertThrows(SessionNotFoundException.class,
                () -> sessionManagerUT.loadByIssuerState(issuerState, FlowVariant.C));
    }

    @DisplayName("Verify valid value issuer_state without Flow")
    @Test
    void test007() {
        var issuerState = TestUtils.generateIssuerState();
        prepareMockInit();
        prepareMockIssuerState(issuerState);
        var session = sessionManagerUT.init(FlowVariant.C);
        session.putParameter(SessionKey.ISSUER_STATE, issuerState);
        sessionManagerUT.persist(session);

        var found = sessionManagerUT.loadByIssuerState(issuerState);
        assertNotNull(found);

        assertEquals(session, found);

        assertThat(session, is(not(sameInstance(found))));

    }

    @DisplayName("Verify null value issuer_state without Flow")
    @Test
    void test008() {
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> sessionManagerUT.loadByIssuerState(null));

        assertAll(
                () -> assertThat(exception.getMessage(), is("invalid issuer_state")),
                () -> assertThat(exception.getLogMessage(), is("Invalid issuer_state: null"))
        );
    }

    @DisplayName("Verify no session with given issuer_state without Flow")
    @Test
    void test009() {
        doThrow(SessionNotFoundException.class).when(sessionAdapter).findByIssuerState(anyString());
        var issuerState = TestUtils.generateIssuerState();
        assertThrows(SessionNotFoundException.class, () -> sessionManagerUT.loadByIssuerState(issuerState));
    }

    @DisplayName("Verify valid value access_token")
    @Test
    void test010() {
        var accessToken = TestUtils.generateAccessToken();
        prepareMockInit();
        prepareMockAccessToken(accessToken);
        var session = sessionManagerUT.init(FlowVariant.C);
        session.putParameter(SessionKey.ACCESS_TOKEN, accessToken);
        sessionManagerUT.persist(session);
        var authorization = "%s %s".formatted(TOKEN_TYPE, accessToken);

        var found = sessionManagerUT.loadByAccessToken(TOKEN_TYPE, authorization, FlowVariant.C);
        assertNotNull(found);
        assertEquals(session, found);
        assertThat(session, is(not(sameInstance(found))));
        verify(sessionAdapter).update(any(), any());
    }

    @DisplayName("Verify null value access_token")
    @Test
    void test011() {
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> sessionManagerUT.loadByAccessToken(TOKEN_TYPE, null, FlowVariant.C));

        assertAll(
                () -> assertThat(exception.getError(), nullValue()),
                () -> assertThat(exception.getScheme(), is(TOKEN_TYPE))
        );
    }

    @DisplayName("Verify no session with given access_token")
    @Test
    void test012() {
        doThrow(SessionNotFoundException.class).when(sessionAdapter).findByAccessToken(anyString());
        var accessToken = TestUtils.generateAccessToken();
        var authorization = "%s %s".formatted(TOKEN_TYPE, accessToken);
        assertThrows(UnauthorizedException.class,
                () -> sessionManagerUT.loadByAccessToken(TOKEN_TYPE, authorization, FlowVariant.C));
    }

    @DisplayName("Verify valid value refresh_token_digest")
    @Test
    void test013() {
        var refreshTokenDigest = TestUtils.generateRefreshTokenDigest();
        prepareMockInit();
        prepareMockRefreshTokenDigest(refreshTokenDigest);
        var session = sessionManagerUT.init(FlowVariant.C1);
        session.putParameter(SessionKey.REFRESH_TOKEN_DIGEST, refreshTokenDigest);
        sessionManagerUT.persist(session);

        var found = sessionManagerUT.loadByRefreshToken(refreshTokenDigest);
        assertNotNull(found);
        assertEquals(session, found);
        assertThat(session, is(not(sameInstance(found))));
        verify(sessionAdapter).update(any(), any());
    }

    @DisplayName("Verify null value refresh_token_digest")
    @Test
    void test014() {
        assertThrows(InvalidRequestException.class,
                () -> sessionManagerUT.loadByRefreshToken(null));
    }

    @DisplayName("Verify no session with given refresh_token_digest")
    @Test
    void test015() {
        doThrow(SessionNotFoundException.class).when(sessionAdapter).findByRefreshTokenDigest(anyString());
        var refreshTokenDigest = TestUtils.generateRefreshTokenDigest();
        assertThrows(SessionNotFoundException.class,
                () -> sessionManagerUT.loadByRefreshToken(refreshTokenDigest));
    }

    @DisplayName("Verify expired sessions deletion called")
    @Test
    void test016() {
        doReturn(42).when(sessionAdapter).deleteExpiredSessions();
        ((AuthorizationHousekeeping) sessionManagerUT).cleanupExpiredSessions();
        verify(sessionAdapter).deleteExpiredSessions();
    }

    private void prepareMockInit() {
        var session = new WSessionImpl(FlowVariant.C, VALID_SESSION_ID);
        doReturn(session).when(sessionAdapter).init(any(), any(), any());
    }

    private void prepareMockRequestUri(String requestUri) {
        var session = new WSessionImpl(FlowVariant.C, VALID_SESSION_ID);
        session.putParameter(SessionKey.REQUEST_URI, requestUri);
        doReturn(session).when(sessionAdapter).findByRequestUri(anyString());
    }

    private void prepareMockAuthorizationCode(String code) {
        var session = new WSessionImpl(FlowVariant.C, VALID_SESSION_ID);
        session.putParameter(SessionKey.AUTHORIZATION_CODE, code);
        doReturn(session).when(sessionAdapter).findByAuthorizationCode(anyString());
    }

    private void prepareMockIssuerState(String issuerState) {
        var session = new WSessionImpl(FlowVariant.C, VALID_SESSION_ID);
        session.putParameter(SessionKey.ISSUER_STATE, issuerState);
        doReturn(session).when(sessionAdapter).findByIssuerState(anyString());
    }

    private void prepareMockAccessToken(String accessToken) {
        var session = new WSessionImpl(FlowVariant.C, VALID_SESSION_ID);
        session.putParameter(SessionKey.ACCESS_TOKEN, accessToken);
        doReturn(session).when(sessionAdapter).findByAccessToken(anyString());
    }

    private void prepareMockRefreshTokenDigest(String refreshTokenDigest) {
        var session = new WSessionImpl(FlowVariant.C1, VALID_SESSION_ID);
        session.putParameter(SessionKey.REFRESH_TOKEN_DIGEST, refreshTokenDigest);
        doReturn(session).when(sessionAdapter).findByRefreshTokenDigest(anyString());
    }
}
