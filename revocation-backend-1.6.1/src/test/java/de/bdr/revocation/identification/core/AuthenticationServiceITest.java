/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core;

import de.bdr.revocation.identification.adapter.out.persistence.AuthenticationAdapter;
import de.bdr.revocation.identification.config.IdentificationConfiguration;
import de.bdr.revocation.identification.core.exception.AuthenticationNotFoundException;
import de.bdr.revocation.identification.core.exception.AuthenticationStateException;
import de.bdr.revocation.identification.core.exception.IllegalTransitionException;
import de.bdr.revocation.identification.core.model.Authentication;
import de.bdr.revocation.identification.core.model.AuthenticationState;
import de.bdr.revocation.identification.core.model.ResponseData;
import de.bdr.revocation.issuance.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static de.bdr.revocation.identification.core.model.AuthenticationState.RESPONDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceITest extends IntegrationTest {

    private static final String TOKEN_ID = "abcdef";
    private static final String RELAY_STATE = "abc123";
    private static final String SAML_ID = RELAY_STATE;
    private static final String SAML_RESPONSE = "nabadafoo";
    private static final String SIG_ALG = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    private static final String SIGNATURE = "abd34283bdc";
    private static final String PSEUDONYM = "defa647492afbac3423423";
    private static final String SESSION_ID = "Session-ID-String";
    private static final String REFERENCE_ID = "Reference-ID-String";
    private static final Instant TIME_PAST = Instant.now().minusSeconds(30);
    private static final Instant CREATED = Instant.now().minusSeconds(3);
    private static final ResponseData RESPONSE_DATA = new ResponseData(PSEUDONYM);

    @MockitoBean
    EidAuth autentMock;

    @MockitoBean
    AuthenticationAdapter authenticationAdapter;

    @Captor
    ArgumentCaptor<Authentication> authenticationArgumentCaptor;

    @Mock
    Authentication authMock;

    @Autowired
    AuthenticationService out;

    @Autowired
    IdentificationConfiguration authenticationConfiguration;

    private static Instant getFutureTime() {
        return Instant.now().plusSeconds(30L);
    }

    @Test
    void when_initializeAuthentication_then_return_Authentication() {
        var result = out.initializeAuthentication();

        assertNotNull(result, "Authentication");
        assertEquals(AuthenticationState.INITIALIZED, result.getAuthenticationState());
    }

    @Test
    void given_tokenId_when_createSamlRedirectBindingUrl_then_call_store_and_autent() {
        when(authMock.getAuthenticationState())
                .thenReturn(AuthenticationState.INITIALIZED);
        when(authMock.getValidUntil())
                .thenReturn(getFutureTime());
        when(authenticationAdapter.findByTokenId(TOKEN_ID))
                .thenReturn(authMock);
        String autentResult = "autentResult";
        when(autentMock.createSamlRedirectBindingUrl(anyString())).thenReturn(autentResult);

        this.out.createSamlRedirectBindingUrl(TOKEN_ID);

        verify(autentMock, times(1)).createSamlRedirectBindingUrl(anyString());
        verify(authenticationAdapter, times(1)).findByTokenId(TOKEN_ID);
        verify(authenticationAdapter, times(1)).updateWithSamlIdentifiedByTokenAndFormerState(authMock, AuthenticationState.INITIALIZED);
        verify(authMock, times(1)).start(anyString());
    }

    @Test
    void given_tokenId_not_in_store_when_createSamlRedirectBindingUrl_then_exception() {
        when(authenticationAdapter.findByTokenId(TOKEN_ID))
                .thenReturn(null);

        assertThrows(AuthenticationNotFoundException.class,
                () -> this.out.createSamlRedirectBindingUrl(TOKEN_ID));
    }

    @Test
    void given_tokenId_null_when_createSamlRedirectBindingUrl_then_IllegalArgumentException() {
        String tokenId = null;
        when(authenticationAdapter.findByTokenId(tokenId))
                .thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> this.out.createSamlRedirectBindingUrl(tokenId));
    }

    @Test
    void given_tokenId_already_Authenticated_when_createSamlRedirectBindingUrl_then_exception() {
        when(authMock.getAuthenticationState())
                .thenReturn(AuthenticationState.AUTHENTICATED);
        when(authenticationAdapter.findByTokenId(TOKEN_ID))
                .thenReturn(authMock);

        assertThrows(AuthenticationStateException.class,
                () -> this.out.createSamlRedirectBindingUrl(TOKEN_ID));
    }

    @Test
    void given_tokenId_validUntil_in_past_when_createSamlRedirectBindingUrl_then_exception() {
        when(authMock.getAuthenticationState())
                .thenReturn(AuthenticationState.INITIALIZED);
        when(authMock.getValidUntil())
                .thenReturn(TIME_PAST);
        when(authenticationAdapter.findByTokenId(TOKEN_ID))
                .thenReturn(authMock);

        assertThrows(AuthenticationStateException.class,
                () -> this.out.createSamlRedirectBindingUrl(TOKEN_ID));
    }

    @Test
    void given_relayState_when_receiveSamlResponse_then_validate_call_store_and_return_referenceId() {
        when(authMock.getAuthenticationState())
                .thenReturn(AuthenticationState.STARTED);
        when(authMock.getValidUntil())
                .thenReturn(getFutureTime());
        when(authenticationAdapter.findBySamlId(RELAY_STATE))
                .thenReturn(authMock);
        var responseData = new ResponseData(PSEUDONYM);
        when(autentMock.validateSamlResponseAndExtractPseudonym(RELAY_STATE, SAML_RESPONSE, SIG_ALG, SIGNATURE, authMock))
                .thenReturn(responseData);

        var result = out.receiveSamlResponse(RELAY_STATE, SAML_RESPONSE, SIG_ALG, SIGNATURE);

        assertNotNull(result, "referenceId");
        verify(authenticationAdapter, times(1)).findBySamlId(RELAY_STATE);
        verify(authMock, times(1)).respond(eq(PSEUDONYM), anyString());
        verify(authenticationAdapter, times(1))
                .updateWithReferenceIdIdentifiedBySamlAndFormerState(authMock, AuthenticationState.STARTED);
    }

    @Test
    void given_auth_empty_when_receiveSamlResponse_then_AuthenticationNotFoundException() {
        assertThrows(AuthenticationNotFoundException.class,
                () -> this.out.receiveSamlResponse("", "", "", ""));
    }

    @Test
    void given_wrong_authState_when_receiveSamlResponse_then_AuthenticationStateException() {
        when(authMock.getAuthenticationState())
                .thenReturn(AuthenticationState.INITIALIZED);
        when(authMock.getValidUntil())
                .thenReturn(getFutureTime());
        when(authenticationAdapter.findBySamlId(RELAY_STATE))
                .thenReturn(authMock);
        when(autentMock.validateSamlResponseAndExtractPseudonym(RELAY_STATE, SAML_RESPONSE, SIG_ALG, SIGNATURE, authMock))
                .thenReturn(RESPONSE_DATA);

        assertThrows(AuthenticationStateException.class,
                () -> this.out.receiveSamlResponse(RELAY_STATE, SAML_RESPONSE, SIG_ALG, SIGNATURE));
    }

    @Test
    void given_authStage_timeout_when_receiveSamlResponse_then_AuthenticationStateException() {
        when(authMock.getAuthenticationState())
                .thenReturn(AuthenticationState.STARTED);
        when(authMock.getValidUntil())
                .thenReturn(TIME_PAST);
        when(authenticationAdapter.findBySamlId(RELAY_STATE))
                .thenReturn(authMock);
        when(autentMock.validateSamlResponseAndExtractPseudonym(RELAY_STATE, SAML_RESPONSE, SIG_ALG, SIGNATURE, authMock))
                .thenReturn(RESPONSE_DATA);

        assertThrows(AuthenticationStateException.class,
                () -> this.out.receiveSamlResponse(RELAY_STATE, SAML_RESPONSE, SIG_ALG, SIGNATURE));
    }

    @Test
    @DisplayName("given sessionId and referenceId when retrieveAuth then auth found")
    void retrieveAuth_ok001() {
        when(authMock.getAuthenticationState()).thenReturn(RESPONDED);
        when(authMock.getReferenceId()).thenReturn(REFERENCE_ID);
        when(authMock.getValidUntil()).thenReturn(getFutureTime());
        when(authenticationAdapter.findBySessionId(SESSION_ID)).thenReturn(authMock);

        var retrievedAuth = this.out.retrieveAuth(SESSION_ID, REFERENCE_ID);

        assertNotNull(retrievedAuth, "Auth not found");
        verify(authenticationAdapter, times(1)).findBySessionId(SESSION_ID);
    }

    @Test
    @DisplayName("given sessionId null when retrieveAuth then AuthenticationNotFoundException")
    void retrieveAuth_fail001() {
        assertThrows(AuthenticationNotFoundException.class,
                () -> this.out.retrieveAuth(null, REFERENCE_ID));

        verify(authenticationAdapter, never()).findBySessionId(null);
    }

    @Test
    @DisplayName("given store returns null when retrieveAuth then AuthenticationNotFoundException")
    void retrieveAuth_fail003() {
        when(authenticationAdapter.findBySessionId(SESSION_ID)).thenReturn(null);

        assertThrows(AuthenticationNotFoundException.class,
                () -> this.out.retrieveAuth(SESSION_ID, REFERENCE_ID));
    }

    @Test
    @DisplayName("given no referenceId and wrong authState when retrieveAuth then AuthenticationStateException")
    void retrieveAuth_fail004() {
        when(authMock.getAuthenticationState()).thenReturn(AuthenticationState.STARTED);
        when(authMock.getReferenceId()).thenReturn(REFERENCE_ID);
        when(authenticationAdapter.findBySessionId(SESSION_ID)).thenReturn(authMock);

        assertThrows(AuthenticationStateException.class,
                () -> this.out.retrieveAuth(SESSION_ID, null));
    }

    @Test
    @DisplayName("given wrong authState when retrieveAuth then AuthenticationStateException")
    void retrieveAuth_fail005() {
        when(authMock.getAuthenticationState()).thenReturn(AuthenticationState.STARTED);
        when(authenticationAdapter.findBySessionId(SESSION_ID)).thenReturn(authMock);

        assertThrows(AuthenticationStateException.class,
                () -> this.out.retrieveAuth(SESSION_ID, REFERENCE_ID));
    }

    @Test
    @DisplayName("given null referenceId and wrong authentication state when retrieveAuth then AuthenticationStateException")
    void retrieveAuth_fail006() {
        when(authMock.getAuthenticationState()).thenReturn(AuthenticationState.STARTED);
        when(authenticationAdapter.findBySessionId(SESSION_ID)).thenReturn(authMock);

        assertThrows(AuthenticationStateException.class,
                () -> this.out.retrieveAuth(SESSION_ID, null));
    }

    @Test
    void given_wrong_authRefId_when_retrieveAuth_then_AuthenticationStateException() {
        when(authMock.getAuthenticationState()).thenReturn(RESPONDED);
        when(authMock.getReferenceId()).thenReturn("Wrong-reference-ID");
        when(authenticationAdapter.findBySessionId(SESSION_ID)).thenReturn(authMock);

        assertThrows(AuthenticationStateException.class,
                () -> this.out.retrieveAuth(SESSION_ID, REFERENCE_ID));
    }

    @Test
    void given_authState_timeout_when_retrieveAuth_then_AuthenticationStateException() {
        when(authMock.getAuthenticationState()).thenReturn(RESPONDED);
        when(authMock.getReferenceId()).thenReturn(REFERENCE_ID);
        when(authMock.getValidUntil()).thenReturn(TIME_PAST);
        when(authenticationAdapter.findBySessionId(SESSION_ID)).thenReturn(authMock);

        assertThrows(AuthenticationStateException.class,
                () -> this.out.retrieveAuth(SESSION_ID, REFERENCE_ID));
    }

    @Test
    void given_authState_responded_when_finishAuthentication_then_storedWithNewSessionId() {
        var realAuth = Authentication.restoreResponded(SESSION_ID, TOKEN_ID, SAML_ID, REFERENCE_ID, PSEUDONYM, getFutureTime(), CREATED);

        out.finishAuthentication(realAuth);

        assertNotEquals(SESSION_ID, realAuth.getSessionId());
        verify(authenticationAdapter, times(1))
                .updateWithChangedSessionValidUntilIdentifiedByReferenceAndFormerState(realAuth, SESSION_ID, RESPONDED);
    }

    @Test
    void given_wrong_authState_when_finishAuthentication_then_IllegalTransitionException() {
        var realAuth = Authentication.initialize(SESSION_ID, TOKEN_ID, getFutureTime());

        assertThrows(IllegalTransitionException.class,
                () -> out.finishAuthentication(realAuth));
    }

    @Test
    void given_sessionId_null_when_terminateAuthentication_then_AuthenticationNotFoundException() {
        assertThrows(AuthenticationNotFoundException.class,
                () -> out.terminateAuthentication(null));
        verify(authenticationAdapter, times(0))
                .removeIdentifiedBySessionAndFormerState(null, AuthenticationState.STARTED);
    }

    @Test
    void given_auth_null_when_terminateAuthentication_then_AuthenticationNotFoundException() {
        when(authenticationAdapter.findBySessionId(SESSION_ID)).thenReturn(null);

        assertThrows(AuthenticationNotFoundException.class,
                () -> out.terminateAuthentication(SESSION_ID));
        verify(authenticationAdapter, times(1)).findBySessionId(SESSION_ID);
        verify(authenticationAdapter, times(0))
                .removeIdentifiedBySessionAndFormerState(SESSION_ID, AuthenticationState.STARTED);
    }

    @Test
    void given_authState_timeout_when_terminateAuthentication_then_exception() {
        when(authenticationAdapter.findBySessionId(SESSION_ID)).thenReturn(authMock);
        when(authMock.getAuthenticationState()).thenReturn(AuthenticationState.TIMEOUT);

        assertThrows(AuthenticationStateException.class,
                () -> out.terminateAuthentication(SESSION_ID));

        verify(authenticationAdapter, times(1)).findBySessionId(SESSION_ID);
        verify(authenticationAdapter, times(0))
                .removeIdentifiedBySessionAndFormerState(SESSION_ID, AuthenticationState.TIMEOUT);
    }

    @Test
    @DisplayName("given wrong authState when terminateAuthentication then AuthenticationStateException")
    void terminateAuthentication_fail002() {
        when(authenticationAdapter.findBySessionId(SESSION_ID)).thenReturn(authMock);
        when(authMock.getAuthenticationState()).thenReturn(AuthenticationState.INITIALIZED);
        when(authMock.getValidUntil()).thenReturn(getFutureTime());

        assertThrows(AuthenticationStateException.class,
                () -> out.terminateAuthentication(SESSION_ID));

        verify(authenticationAdapter, times(0))
                .removeIdentifiedBySessionAndFormerState(SESSION_ID, AuthenticationState.INITIALIZED);
    }

    @Test
    @DisplayName("given valid until timed out when terminateAuthentication then AuthenticationStateException")
    void terminateAuthentication_fail003() {
        when(authenticationAdapter.findBySessionId(SESSION_ID)).thenReturn(authMock);
        when(authMock.getAuthenticationState()).thenReturn(AuthenticationState.AUTHENTICATED);
        when(authMock.getValidUntil()).thenReturn(TIME_PAST);

        assertThrows(AuthenticationStateException.class,
                () -> out.terminateAuthentication(SESSION_ID));

        verify(authenticationAdapter, times(0))
                .removeIdentifiedBySessionAndFormerState(SESSION_ID, AuthenticationState.INITIALIZED);
    }

    @Test
    @DisplayName("given authState authenticated when terminateAuthentication then terminated and removed")
    void terminateAuthentication_ok001() {
        var realAuth = Authentication.restoreAuthenticated(SESSION_ID, TOKEN_ID, SAML_ID, REFERENCE_ID, PSEUDONYM, getFutureTime(), CREATED);

        when(authenticationAdapter.findBySessionId(SESSION_ID)).thenReturn(realAuth);

        assertDoesNotThrow(() -> out.terminateAuthentication(SESSION_ID));
        assertEquals(AuthenticationState.TERMINATED, realAuth.getAuthenticationState());
        verify(authenticationAdapter, times(1))
                .removeIdentifiedBySessionAndFormerState(SESSION_ID, AuthenticationState.AUTHENTICATED);
    }

    @Test
    @DisplayName("given authState authenticated when extendAuthenticationValidity then sets correct new valid until")
    void extendAuthenticationValidity_ok001() {
        var realAuth = Authentication.restoreAuthenticated(SESSION_ID, TOKEN_ID, SAML_ID, REFERENCE_ID, PSEUDONYM, getFutureTime(), CREATED);

        var now = Instant.now();

        out.extendAuthenticationValidity(realAuth);

        verify(authenticationAdapter).updateValidUntil(authenticationArgumentCaptor.capture());
        var extendedAuthentication = authenticationArgumentCaptor.getValue();

        assertThat(extendedAuthentication.getValidUntil()).isAfter(now);
        var effective = Duration.between(now, extendedAuthentication.getValidUntil()).getSeconds();
        assertTrue(authenticationConfiguration.getMinAuthenticatedSessionDuration().getSeconds() - effective <= 1);
    }

    @Test
    @DisplayName("given authState authenticated when extendAuthenticationValidity and validUntil short before maximum then sets correct maximum valid Until")
    void extendAuthenticationValidity_ok002() {
        var createdInPast = CREATED.minusSeconds(authenticationConfiguration.getMaxAuthenticatedSessionDuration().getSeconds())
                .plusSeconds(120);
        var validUntilMaximum = createdInPast.plusSeconds(authenticationConfiguration.getMaxAuthenticatedSessionDuration().getSeconds());
        var validUntilShortBeforeMaximumValidity = Instant.now().plusSeconds(20);
        var auth = Authentication.restoreAuthenticated(SESSION_ID, TOKEN_ID, SAML_ID, REFERENCE_ID, PSEUDONYM,
                validUntilShortBeforeMaximumValidity,
                createdInPast);
        var oldValidUntil = auth.getValidUntil();

        out.extendAuthenticationValidity(auth);

        verify(authenticationAdapter).updateValidUntil(authenticationArgumentCaptor.capture());
        var extendedAuthentication = authenticationArgumentCaptor.getValue();

        assertThat(extendedAuthentication.getValidUntil()).isAfter(oldValidUntil);
        assertThat(extendedAuthentication.getValidUntil()).isEqualTo(validUntilMaximum);
    }

    @Test
    @DisplayName("given authState other than authenticated when extendAuthenticationValidity then exception")
    void extendAuthenticationValidity_fail001() {
        var auth = Authentication.restoreTerminated(SESSION_ID, TOKEN_ID, SAML_ID, REFERENCE_ID, getFutureTime(), CREATED);

        assertThrows(AuthenticationStateException.class, () -> out.extendAuthenticationValidity(auth));

        verify(authenticationAdapter, never()).updateValidUntil(any());
    }
}