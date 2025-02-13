/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.Builder;

import static de.bdr.revocation.identification.core.model.Authentication.restoreAuthenticated;
import static de.bdr.revocation.identification.core.model.Authentication.restoreInitialized;
import static de.bdr.revocation.identification.core.model.Authentication.restoreResponded;
import static de.bdr.revocation.identification.core.model.Authentication.restoreStarted;
import static de.bdr.revocation.identification.core.model.Authentication.restoreTerminated;
import static de.bdr.revocation.identification.core.model.Authentication.restoreTimeout;

public class ModelTestData {
    @Builder(builderMethodName = "authenticationBuilder")
    public static Authentication buildAuthentication(
            AuthenticationState authenticationState,
            String sessionId,
            String samlId,
            String tokenId,
            String referenceId,
            String pseudonym,
            Instant created,
            Instant validUntil
    ) {
        return switch (authenticationState) {
            case INITIALIZED -> restoreInitialized(sessionId, tokenId, validUntil, created);
            case STARTED -> restoreStarted(sessionId, tokenId, samlId, validUntil, created);
            case RESPONDED -> restoreResponded(sessionId, tokenId, samlId, referenceId, pseudonym, validUntil, created);
            case AUTHENTICATED -> restoreAuthenticated(sessionId, tokenId, samlId, referenceId, pseudonym, validUntil, created);
            case TIMEOUT -> restoreTimeout(sessionId, tokenId, samlId, referenceId, validUntil, created);
            case TERMINATED -> restoreTerminated(sessionId, tokenId, samlId, referenceId, validUntil, created);
        };
    }

    public static AuthenticationBuilder defaultAuthentication() {
        var now = Instant.now();
        return authenticationBuilder()
                .authenticationState(AuthenticationState.INITIALIZED)
                .sessionId("TestSessionId")
                .tokenId("TestTokenId")
                .samlId("TestSamlId")
                .referenceId("TestReferenceId")
                .pseudonym("TestPseudonym")
                .created(now)
                .validUntil(now.plus(300, ChronoUnit.SECONDS));
    }
}
