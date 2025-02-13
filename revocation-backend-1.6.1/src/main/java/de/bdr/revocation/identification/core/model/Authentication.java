/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.model;

import de.bdr.revocation.identification.core.exception.IllegalTransitionException;
import de.bdr.revocation.identification.core.exception.IllegalValidityExtensionException;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Getter
public class Authentication {
    public static final String CREATED_NOT_NULL = "created must not be null";
    public static final String AUTHENTICATION_STATE_NOT_NULL = "authenticationState must not be null";
    public static final String SESSION_ID_NOT_NULL = "sessionId must not be null";
    public static final String TOKEN_ID_NOT_NULL = "tokenId must not be null";
    public static final String VALID_UNTIL_NOT_NULL = "validUntil must not be null";
    public static final String SAML_ID_NOT_NULL = "samlId must not be null";
    public static final String REFERENCE_ID_NOT_NULL = "referenceId must not be null";
    public static final String PSEUDONYM_NOT_NULL = "pseudonym must not be null";

    public static final int ID_BYTES = 16;

    private AuthenticationState authenticationState;

    private String sessionId;
    private final String tokenId;
    private String samlId;
    private String referenceId;
    private String pseudonym;
    private Instant created;
    private Instant validUntil;

    private Authentication(AuthenticationState authenticationState, String sessionId, String tokenId,
                           Instant validUntil) {
        this.authenticationState = Objects.requireNonNull(authenticationState, AUTHENTICATION_STATE_NOT_NULL);
        this.sessionId = Objects.requireNonNull(sessionId, SESSION_ID_NOT_NULL);
        this.tokenId = Objects.requireNonNull(tokenId, TOKEN_ID_NOT_NULL);
        this.validUntil = Objects.requireNonNull(validUntil, VALID_UNTIL_NOT_NULL);
        this.created = Instant.now();
    }

    /**
     * Factory method for a freshly initialized Authentication.
     *
     * @param sessionId  identifies the HTTP session, must not be NULL
     * @param tokenId    identifier handed to the eID client, must not be NULL
     * @param validUntil the Authentication times out at this point in time, must not be NULL
     */
    public static Authentication initialize(String sessionId, String tokenId, Instant validUntil) {
        return new Authentication(AuthenticationState.INITIALIZED, sessionId, tokenId, validUntil);
    }

    /**
     * Factory method to restore an Authentication in INITIALIZED status.
     *
     * @param sessionId  identifies the HTTP session, must not be NULL
     * @param tokenId    identifier handed to the eID client, must not be NULL
     * @param validUntil the Authentication times out at this point in time, must not be NULL
     * @param created    the creation time, must not be NULL
     */
    public static Authentication restoreInitialized(String sessionId, String tokenId,
                                                    Instant validUntil, Instant created) {
        var result = new Authentication(AuthenticationState.INITIALIZED, sessionId, tokenId, validUntil);
        result.created = Objects.requireNonNull(created, CREATED_NOT_NULL);
        return result;
    }

    /**
     * Factory method to restore an Authentication in STARTED status.
     *
     * @param sessionId  identifies the HTTP session, must not be NULL
     * @param tokenId    identifier handed to the eID client, must not be NULL
     * @param samlId     identifier for the SAML request, must not be NULL
     * @param validUntil the Authentication times out at this point in time, must not be NULL
     * @param created    the creation time, must not be NULL
     */
    public static Authentication restoreStarted(String sessionId, String tokenId, String samlId,
                                                Instant validUntil, Instant created) {
        var result = new Authentication(AuthenticationState.STARTED, sessionId, tokenId, validUntil);
        result.samlId = Objects.requireNonNull(samlId, SAML_ID_NOT_NULL);
        result.created = Objects.requireNonNull(created, CREATED_NOT_NULL);
        return result;
    }

    /**
     * Factory method to restore an Authentication in RESPONDED status.
     *
     * @param sessionId   identifies the HTTP session, must not be NULL
     * @param tokenId     identifier handed to the eID client, must not be NULL
     * @param samlId      identifier for the SAML request, must not be NULL
     * @param referenceId identifier handed to the LoggedIn page, must not be NULL
     * @param pseudonym   the pseudonym as read from the eID, must not be NULL
     * @param validUntil  the Authentication times out at this point in time, must not be NULL
     * @param created     the creation time, must not be NULL
     */
    public static Authentication restoreResponded(String sessionId, String tokenId, String samlId,
                                                  String referenceId, String pseudonym, Instant validUntil,
                                                  Instant created) {
        var result = new Authentication(AuthenticationState.RESPONDED, sessionId, tokenId, validUntil);
        return setAuthenticationValues(samlId, referenceId, pseudonym, created, result);
    }

    /**
     * Factory method to restore an Authentication in AUTHENTICATED status.
     *
     * @param sessionId   identifies the HTTP session, must not be NULL
     * @param tokenId     identifier handed to the eID client, must not be NULL
     * @param samlId      identifier for the SAML request, must not be NULL
     * @param referenceId identifier handed to the LoggedIn page, must not be NULL
     * @param pseudonym   the pseudonym as read from the eID, must not be NULL
     * @param validUntil  the Authentication times out at this point in time, must not be NULL
     * @param created     the creation time, must not be NULL
     */
    public static Authentication restoreAuthenticated(String sessionId, String tokenId, String samlId,
                                                      String referenceId, String pseudonym, Instant validUntil,
                                                      Instant created) {
        var result = new Authentication(AuthenticationState.AUTHENTICATED, sessionId, tokenId, validUntil);
        return setAuthenticationValues(samlId, referenceId, pseudonym, created, result);
    }

    private static Authentication setAuthenticationValues(String samlId, String referenceId, String pseudonym, Instant created, Authentication result) {
        result.samlId = Objects.requireNonNull(samlId, SAML_ID_NOT_NULL);
        result.referenceId = Objects.requireNonNull(referenceId, REFERENCE_ID_NOT_NULL);
        result.pseudonym = Objects.requireNonNull(pseudonym, PSEUDONYM_NOT_NULL);
        result.created = Objects.requireNonNull(created, CREATED_NOT_NULL);
        return result;
    }

    /**
     * Factory method to restore an Authentication in TIMEOUT status.
     * Not every parameter may have a value and the resulting object cannot be used
     * for service access anymore (no valid pseudonym)
     *
     * @param sessionId   identifies the HTTP session, must not be NULL
     * @param tokenId     identifier handed to the eID client, must not be NULL
     * @param samlId      identifier for the SAML request
     * @param referenceId identifier handed to the LoggedIn page
     * @param validUntil  the Authentication times out at this point in time, must not be NULL
     * @param created     the creation time, must not be NULL
     */
    public static Authentication restoreTimeout(String sessionId, String tokenId, String samlId,
                                                String referenceId, Instant validUntil, Instant created) {
        var result = new Authentication(AuthenticationState.TIMEOUT, sessionId, tokenId, validUntil);
        result.samlId = samlId;
        result.referenceId = referenceId;
        result.created = Objects.requireNonNull(created, CREATED_NOT_NULL);
        return result;
    }

    /**
     * Factory method to restore an Authentication in TERMINATED status.
     * Not every parameter may have a value and the resulting object cannot be used
     * for service access anymore (no valid pseudonym)
     *
     * @param sessionId   identifies the HTTP session, must not be NULL
     * @param tokenId     identifier handed to the eID client, must not be NULL
     * @param samlId      identifier for the SAML request
     * @param referenceId identifier handed to the LoggedIn page
     * @param validUntil  the Authentication times out at this point in time, must not be NULL
     * @param created     (not null)
     */
    public static Authentication restoreTerminated(String sessionId, String tokenId, String samlId,
                                                   String referenceId, Instant validUntil, Instant created) {
        var result = new Authentication(AuthenticationState.TERMINATED, sessionId, tokenId, validUntil);
        result.samlId = samlId;
        result.referenceId = referenceId;
        result.created = Objects.requireNonNull(created, CREATED_NOT_NULL);
        return result;
    }

    public synchronized void start(String samlId) {
        if (this.authenticationState != AuthenticationState.INITIALIZED) {
            throwIllegalTransitionException();
        }
        this.authenticationState = AuthenticationState.STARTED;
        this.samlId = samlId;
    }

    private void throwIllegalTransitionException() {
        throw new IllegalTransitionException("cannot perform transition, current state is "
                + this.authenticationState.name());
    }

    public synchronized void respond(String pseudonym, String referenceId) {
        if (this.authenticationState != AuthenticationState.STARTED) {
            throwIllegalTransitionException();
        }
        this.authenticationState = AuthenticationState.RESPONDED;
        this.pseudonym = pseudonym;
        this.referenceId = referenceId;
    }

    public synchronized void authenticate(String sessionId, Instant validUntil) {
        if (this.authenticationState != AuthenticationState.RESPONDED) {
            throwIllegalTransitionException();
        }
        this.authenticationState = AuthenticationState.AUTHENTICATED;
        this.sessionId = sessionId;
        this.validUntil = validUntil;
    }

    public synchronized void extendTo(Instant validUntil) {
        if (this.authenticationState != AuthenticationState.AUTHENTICATED) {
            throw new IllegalValidityExtensionException("cannot perform validity extension, current state is "
                    + this.authenticationState.name());
        }
        this.validUntil = validUntil;
    }

    public synchronized void terminate() {
        if (this.authenticationState != AuthenticationState.AUTHENTICATED) {
            throwIllegalTransitionException();
        }
        this.authenticationState = AuthenticationState.TERMINATED;
    }

    public synchronized void timeout(Instant inspectionTime) {
        if (this.authenticationState == AuthenticationState.TERMINATED
                || this.authenticationState == AuthenticationState.TIMEOUT) {
            return;
        }
        if (this.validUntil.isBefore(inspectionTime)) {
            this.authenticationState = AuthenticationState.TIMEOUT;
        }
    }

    public synchronized int getRemainingValidityInSeconds() {
        return (int) Duration.between(Instant.now(), getValidUntil()).toSeconds();
    }
}
