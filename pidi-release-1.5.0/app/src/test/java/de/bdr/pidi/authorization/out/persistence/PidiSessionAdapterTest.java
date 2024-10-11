/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.out.persistence;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.SessionNotFoundException;
import de.bdr.pidi.end2end.integration.IntegrationTest;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static de.bdr.pidi.authorization.ConfigTestData.AUTH_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PidiSessionAdapterTest extends IntegrationTest {

    @Autowired
    private PidiSessionAdapter sessionAdapter;

    @Autowired
    private PidiSessionRepository sessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify new session gets initialized and persisted")
    void test001() {
        var session = sessionAdapter.init(FlowVariant.C, Requests.PUSHED_AUTHORIZATION_REQUEST, AUTH_CONFIG.getSessionExpirationTime());
        assertThat(session.getSessionId()).isPositive();
        assertThat(session.getFlowVariant()).isEqualTo(FlowVariant.C);
        assertThat(session.getNextExpectedRequest()).isEqualTo(Requests.PUSHED_AUTHORIZATION_REQUEST);
        assertThat(sessionRepository.existsById(session.getSessionId())).isTrue();
    }

    @Test
    @DisplayName("Verify session request_uri gets persisted")
    void test002() {
        var requestUri = TestUtils.generateRequestUri();
        var session = sessionAdapter.init(FlowVariant.C, Requests.PUSHED_AUTHORIZATION_REQUEST, AUTH_CONFIG.getSessionExpirationTime());
        session.putParameter(SessionKey.REQUEST_URI, requestUri);
        session.setNextExpectedRequest(Requests.AUTHORIZATION_REQUEST);
        sessionAdapter.update(session, AUTH_CONFIG.getSessionExpirationTime());

        var optional = sessionRepository.findById(session.getSessionId());
        assertThat(optional).isPresent();
        var entity = optional.get();
        assertThat(entity.getRequestUri()).isEqualTo(requestUri);
        assertThat(entity.getNextExpectedRequest()).isEqualTo(Requests.AUTHORIZATION_REQUEST);
    }

    @Test
    @DisplayName("Verify session gets found by request_uri")
    void test003() {
        var requestUri = TestUtils.generateRequestUri();
        var session = sessionAdapter.init(FlowVariant.C, Requests.PUSHED_AUTHORIZATION_REQUEST, AUTH_CONFIG.getSessionExpirationTime());
        session.putParameter(SessionKey.REQUEST_URI, requestUri);
        session.setNextExpectedRequest(Requests.AUTHORIZATION_REQUEST);
        sessionAdapter.update(session, AUTH_CONFIG.getSessionExpirationTime());

        var foundSession = sessionAdapter.findByRequestUri(requestUri);
        assertThat(foundSession.getParameter(SessionKey.REQUEST_URI)).isEqualTo(requestUri);
        assertThat(foundSession.getNextExpectedRequest()).isEqualTo(Requests.AUTHORIZATION_REQUEST);
    }

    @Test
    @DisplayName("Verify session gets not found by request_uri and throws exception")
    void test004() {
        var requestUri = TestUtils.generateRequestUri();
        assertThatThrownBy(() -> sessionAdapter.findByRequestUri(requestUri))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    @DisplayName("Verify session authorization code gets persisted")
    void test005() {
        var authCode = TestUtils.generateAuthorizationCode();
        var session = sessionAdapter.init(FlowVariant.C, Requests.IDENTIFICATION_RESULT, AUTH_CONFIG.getSessionExpirationTime());
        session.putParameter(SessionKey.AUTHORIZATION_CODE, authCode);
        session.setNextExpectedRequest(Requests.TOKEN_REQUEST);
        sessionAdapter.update(session, AUTH_CONFIG.getSessionExpirationTime());

        var optional = sessionRepository.findById(session.getSessionId());
        assertThat(optional).isPresent();
        var entity = optional.get();
        assertThat(entity.getAuthorizationCode()).isEqualTo(authCode);
        assertThat(entity.getNextExpectedRequest()).isEqualTo(Requests.TOKEN_REQUEST);
    }

    @Test
    @DisplayName("Verify session gets found by authorization code")
    void test006() {
        var authCode = TestUtils.generateAuthorizationCode();
        var session = sessionAdapter.init(FlowVariant.C, Requests.IDENTIFICATION_RESULT, AUTH_CONFIG.getSessionExpirationTime());
        session.putParameter(SessionKey.AUTHORIZATION_CODE, authCode);
        session.setNextExpectedRequest(Requests.TOKEN_REQUEST);
        sessionAdapter.update(session, AUTH_CONFIG.getSessionExpirationTime());

        var foundSession = sessionAdapter.findByAuthorizationCode(authCode);
        assertThat(foundSession.getParameter(SessionKey.AUTHORIZATION_CODE)).isEqualTo(authCode);
        assertThat(foundSession.getNextExpectedRequest()).isEqualTo(Requests.TOKEN_REQUEST);
    }

    @Test
    @DisplayName("Verify session gets not found by authorization code and throws exception")
    void test007() {
        var authCode = TestUtils.generateAuthorizationCode();
        assertThatThrownBy(() -> sessionAdapter.findByAuthorizationCode(authCode))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    @DisplayName("Verify session issuer_state gets persisted")
    void test008() {
        var issuerState = TestUtils.generateIssuerState();
        var session = sessionAdapter.init(FlowVariant.C, Requests.AUTHORIZATION_REQUEST, AUTH_CONFIG.getSessionExpirationTime());
        session.putParameter(SessionKey.ISSUER_STATE, issuerState);
        session.setNextExpectedRequest(Requests.IDENTIFICATION_RESULT);
        sessionAdapter.update(session, AUTH_CONFIG.getSessionExpirationTime());

        var optional = sessionRepository.findById(session.getSessionId());
        assertThat(optional).isPresent();
        var entity = optional.get();
        assertThat(entity.getIssuerState()).isEqualTo(issuerState);
        assertThat(entity.getNextExpectedRequest()).isEqualTo(Requests.IDENTIFICATION_RESULT);
    }

    @Test
    @DisplayName("Verify session gets found by issuer_state")
    void test009() {
        var issuerState = TestUtils.generateIssuerState();
        var session = sessionAdapter.init(FlowVariant.C, Requests.AUTHORIZATION_REQUEST, AUTH_CONFIG.getSessionExpirationTime());
        session.putParameter(SessionKey.ISSUER_STATE, issuerState);
        session.setNextExpectedRequest(Requests.IDENTIFICATION_RESULT);
        sessionAdapter.update(session, AUTH_CONFIG.getSessionExpirationTime());

        var foundSession = sessionAdapter.findByIssuerState(issuerState);
        assertThat(foundSession.getParameter(SessionKey.ISSUER_STATE)).isEqualTo(issuerState);
        assertThat(foundSession.getNextExpectedRequest()).isEqualTo(Requests.IDENTIFICATION_RESULT);
    }

    @Test
    @DisplayName("Verify session gets not found by issuer_state and throws exception")
    void test010() {
        var issuerState = TestUtils.generateIssuerState();
        assertThatThrownBy(() -> sessionAdapter.findByIssuerState(issuerState))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    @DisplayName("Verify session access_token gets persisted")
    void test011() {
        var accessToken = TestUtils.generateAccessToken();
        var session = sessionAdapter.init(FlowVariant.C, Requests.TOKEN_REQUEST, AUTH_CONFIG.getSessionExpirationTime());
        session.putParameter(SessionKey.ACCESS_TOKEN, accessToken);
        session.setNextExpectedRequest(Requests.CREDENTIAL_REQUEST);
        sessionAdapter.update(session, AUTH_CONFIG.getSessionExpirationTime());

        var optional = sessionRepository.findById(session.getSessionId());
        assertThat(optional).isPresent();
        var entity = optional.get();
        assertThat(entity.getAccessToken()).isEqualTo(accessToken);
        assertThat(entity.getNextExpectedRequest()).isEqualTo(Requests.CREDENTIAL_REQUEST);
    }

    @Test
    @DisplayName("Verify session gets found by access_token")
    void test012() {
        var accessToken = TestUtils.generateAccessToken();
        var session = sessionAdapter.init(FlowVariant.C, Requests.TOKEN_REQUEST, AUTH_CONFIG.getSessionExpirationTime());
        session.putParameter(SessionKey.ACCESS_TOKEN, accessToken);
        session.setNextExpectedRequest(Requests.CREDENTIAL_REQUEST);
        sessionAdapter.update(session, AUTH_CONFIG.getSessionExpirationTime());

        var foundSession = sessionAdapter.findByAccessToken(accessToken);
        assertThat(foundSession.getParameter(SessionKey.ACCESS_TOKEN)).isEqualTo(accessToken);
        assertThat(foundSession.getNextExpectedRequest()).isEqualTo(Requests.CREDENTIAL_REQUEST);
    }

    @Test
    @DisplayName("Verify session gets not found by access_token and throws exception")
    void test013() {
        var accessToken = TestUtils.generateAccessToken();
        assertThatThrownBy(() -> sessionAdapter.findByAccessToken(accessToken))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    @DisplayName("Verify session access_token gets persisted")
    void test014() {
        var refreshTokenDigest = TestUtils.generateAccessToken();
        var session = sessionAdapter.init(FlowVariant.C1, Requests.TOKEN_REQUEST, AUTH_CONFIG.getSessionExpirationTime());
        session.putParameter(SessionKey.REFRESH_TOKEN_DIGEST, refreshTokenDigest);
        session.setNextExpectedRequest(Requests.CREDENTIAL_REQUEST);
        sessionAdapter.update(session, AUTH_CONFIG.getSessionExpirationTime());

        var optional = sessionRepository.findById(session.getSessionId());
        assertThat(optional).isPresent();
        var entity = optional.get();
        assertThat(entity.getRefreshTokenDigest()).isEqualTo(refreshTokenDigest);
        assertThat(entity.getNextExpectedRequest()).isEqualTo(Requests.CREDENTIAL_REQUEST);
    }

    @Test
    @DisplayName("Verify session gets found by refresh_token_digest")
    void test015() {
        var refreshTokenDigest = TestUtils.generateRefreshTokenDigest();
        var session = sessionAdapter.init(FlowVariant.C1, Requests.TOKEN_REQUEST, AUTH_CONFIG.getSessionExpirationTime());
        session.putParameter(SessionKey.REFRESH_TOKEN_DIGEST, refreshTokenDigest);
        session.setNextExpectedRequest(Requests.CREDENTIAL_REQUEST);
        sessionAdapter.update(session, AUTH_CONFIG.getSessionExpirationTime());

        var foundSession = sessionAdapter.findByRefreshTokenDigest(refreshTokenDigest);
        assertThat(foundSession.getParameter(SessionKey.REFRESH_TOKEN_DIGEST)).isEqualTo(refreshTokenDigest);
        assertThat(foundSession.getNextExpectedRequest()).isEqualTo(Requests.CREDENTIAL_REQUEST);
    }

    @Test
    @DisplayName("Verify session gets not found by access_token and throws exception")
    void test016() {
        var refreshTokenDigest = TestUtils.generateRefreshTokenDigest();
        assertThatThrownBy(() -> sessionAdapter.findByRefreshTokenDigest(refreshTokenDigest))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    @DisplayName("Verify session parameters get mapped using the SessionKey value (not enum name)")
    void test017() {
        var session = sessionAdapter.init(FlowVariant.C, Requests.PUSHED_AUTHORIZATION_REQUEST, AUTH_CONFIG.getSessionExpirationTime());
        var requestUri = TestUtils.generateRequestUri();
        session.putParameter(SessionKey.REQUEST_URI, requestUri);
        sessionAdapter.update(session, AUTH_CONFIG.getSessionExpirationTime());

        var params = jdbcTemplate.queryForObject("select session from pidi_session where id = ?", String.class, session.getSessionId());
        var expected = "{\"%s\":\"%s\"}".formatted(SessionKey.REQUEST_URI.getValue(), requestUri);
        assertThat(params).isEqualToIgnoringWhitespace(expected);
    }
}
