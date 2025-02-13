/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.model;

import de.bdr.revocation.identification.core.exception.IllegalTransitionException;
import de.bdr.revocation.identification.core.exception.IllegalValidityExtensionException;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationTest {

    private static final int SHORT_FUTURE = 5;
    private static final int NEAR_FUTURE = 20;
    private static final int FAR_FUTURE = 200;

    String sessionId, sessionId2;
    String tokenId;
    String samlId, samlId2;
    String pseudonym;
    String referenceId;
    Instant ttl, ttl2, created;
    @BeforeEach
    void setup() {
        sessionId = "ab1234";
        sessionId2 = "343abc";
        tokenId = "cd3456";
        samlId = "23bb-23ee";
        samlId2 = "23bb-2e3f";
        pseudonym = "12abcd34";
        referenceId = "1a2b3c4d";
        created = now().minusSeconds(1);
        ttl = now().plusSeconds(NEAR_FUTURE);
        ttl2 = now().plusSeconds(SHORT_FUTURE);
    }

    @Test
    void when_initialize_then_Authentication_status_Initialized() {

        var auth = Authentication.initialize(sessionId, tokenId, ttl);

        assertEquals(AuthenticationState.INITIALIZED, auth.getAuthenticationState());
        assertEquals(sessionId, auth.getSessionId());
        assertEquals(tokenId, auth.getTokenId());
        assertNull(auth.getSamlId());
        assertNotNull(auth.getCreated());
    }

    @Test
    void given_Initialized_when_start_then_Authentication_status_Started() {

        var auth = Authentication.restoreInitialized(sessionId, tokenId, ttl, created);

        auth.start(samlId);

        assertEquals(AuthenticationState.STARTED, auth.getAuthenticationState());
        assertEquals(sessionId, auth.getSessionId());
        assertEquals(tokenId, auth.getTokenId());
        assertEquals(samlId, auth.getSamlId());
    }

    @Test
    void given_Started_when_start_then_exception() {

        var auth = givenAuthenticationStarted();

        assertThrows(IllegalTransitionException.class, () -> auth.start(samlId2));

        assertThrows(IllegalTransitionException.class, () -> auth.start(samlId));
    }

    private Authentication givenAuthenticationStarted() {
        return Authentication.restoreStarted(sessionId, tokenId, samlId, ttl, created);
    }

    @Test
    void given_Started_when_respond_then_Authentication_status_Responded() {

        var auth = givenAuthenticationStarted();

        auth.respond(pseudonym, referenceId);

        assertEquals(AuthenticationState.RESPONDED, auth.getAuthenticationState());
        assertEquals(sessionId, auth.getSessionId());
        assertEquals(tokenId, auth.getTokenId());
        assertEquals(samlId, auth.getSamlId());
        assertEquals(pseudonym, auth.getPseudonym());
        assertEquals(referenceId, auth.getReferenceId());
    }

    @Test
    void given_Responded_when_authenticate_then_Authentication_status_Authenticated() {

        var auth = givenAuthenticationResponded();

        auth.authenticate(sessionId2, ttl2);

        assertEquals(AuthenticationState.AUTHENTICATED, auth.getAuthenticationState());
        assertEquals(sessionId2, auth.getSessionId());
        assertEquals(tokenId, auth.getTokenId());
        assertEquals(samlId, auth.getSamlId());
        assertEquals(pseudonym, auth.getPseudonym());
        assertEquals(referenceId, auth.getReferenceId());
        assertEquals(ttl2, auth.getValidUntil());
    }

    @Test
    void given_Started_when_timeout_past_then_Authentication_status_Timeout() {

        var auth = givenAuthenticationStarted();

        auth.timeout(now().plusSeconds(FAR_FUTURE));

        assertEquals(AuthenticationState.TIMEOUT, auth.getAuthenticationState());
        assertEquals(sessionId, auth.getSessionId());
        assertEquals(tokenId, auth.getTokenId());
        assertEquals(samlId, auth.getSamlId());
    }

    @Test
    void given_Authenticated_when_terminate_then_Authentication_status_Terminated() {

        var auth = givenAuthenticationAuthenticated();

        auth.terminate();

        assertEquals(AuthenticationState.TERMINATED, auth.getAuthenticationState());
        assertEquals(sessionId, auth.getSessionId());
        assertEquals(tokenId, auth.getTokenId());
        assertEquals(samlId, auth.getSamlId());
        assertEquals(pseudonym, auth.getPseudonym());
        assertEquals(referenceId, auth.getReferenceId());
    }

    @Test
    void when_restoreTimeout_then_withoutPseudonym() {

        created = now().minusSeconds(2);
        Instant past = now().minusSeconds(1);

        var auth = Authentication.restoreTimeout(sessionId, tokenId, null, null, past, created);

        assertEquals(AuthenticationState.TIMEOUT, auth.getAuthenticationState());
        assertEquals(sessionId, auth.getSessionId());
        assertEquals(tokenId, auth.getTokenId());
        assertNull(auth.getSamlId());
        assertNull(auth.getPseudonym());
        assertNull(auth.getReferenceId());
        assertEquals(past, auth.getValidUntil());
        assertEquals(created, auth.getCreated());
    }

    @Test
    void given_Authenticated_when_extendTo_then_validUntil_is_extended() {
        var auth = givenAuthenticationAuthenticated();
        var oldValidUntil = auth.getValidUntil();
        auth.extendTo(auth.getValidUntil().plusSeconds(600));
        assertThat(auth.getValidUntil()).isEqualTo(oldValidUntil.plusSeconds(600));
    }

    @Test
    void given_Timedout_when_extendTo_then_exception() {
        created = now().minusSeconds(2);
        Instant past = now().minusSeconds(1);

        var auth = Authentication.restoreTimeout(sessionId, tokenId, null, null, past, created);
        var extendedValidUntil = past.plusSeconds(600);
        assertThrows(IllegalValidityExtensionException.class, () -> auth.extendTo(extendedValidUntil));
    }

    @Test
    void when_restoreTerminated_then_withoutPseudonym() {

        created = now().minusSeconds(2);
        Instant past = now().minusSeconds(1);

        var auth = Authentication.restoreTerminated(sessionId2, tokenId, samlId, referenceId, past, created);

        assertEquals(AuthenticationState.TERMINATED, auth.getAuthenticationState());
        assertEquals(sessionId2, auth.getSessionId());
        assertEquals(tokenId, auth.getTokenId());
        assertEquals(samlId, auth.getSamlId());
        assertNull(auth.getPseudonym());
        assertEquals(referenceId, auth.getReferenceId());
        assertEquals(past, auth.getValidUntil());
    }

    @NotNull
    private Authentication givenAuthenticationResponded() {
        return Authentication.restoreResponded(sessionId, tokenId, samlId, referenceId, pseudonym, ttl, created);
    }

    @NotNull
    private Authentication givenAuthenticationAuthenticated() {
        return Authentication.restoreAuthenticated(sessionId, tokenId, samlId, referenceId, pseudonym, ttl, created);
    }

}