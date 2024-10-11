/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core.model;

import de.bdr.pidi.identification.core.exception.IllegalTransitionException;
import de.bdr.pidi.identification.core.exception.IllegalValidityExtensionException;
import lombok.Getter;

import java.net.URL;
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

    public static final int ID_BYTES = 16;
    public static final int ID_CHARS = 2 * ID_BYTES;

    private final String tokenId;
    private final String externalId;
    private AuthenticationState authenticationState;
    private String sessionId;
    private String samlId;
    private String referenceId;
    private Instant created;
    private Instant validUntil;
    private final URL redirectUrl;

    private Authentication(AuthenticationState authenticationState, String sessionId, String tokenId,
                           Instant validUntil, String externalId, URL redirectUrl) {
        this.authenticationState = Objects.requireNonNull(authenticationState, AUTHENTICATION_STATE_NOT_NULL);
        this.sessionId = Objects.requireNonNull(sessionId, SESSION_ID_NOT_NULL);
        this.tokenId = Objects.requireNonNull(tokenId, TOKEN_ID_NOT_NULL);
        this.validUntil = Objects.requireNonNull(validUntil, VALID_UNTIL_NOT_NULL);
        this.redirectUrl = redirectUrl;
        this.created = Instant.now();
        this.externalId = externalId;
    }

    /**
     * Factory method for a freshly initialized Authentication.
     *
     * @param sessionId   identifies the HTTP session, must not be NULL
     * @param tokenId     identifier handed to the eID client, must not be NULL
     * @param validUntil  the Authentication times out at this point in time, must not be NULL
     * @param externalId  an id from the system which request the authentication
     * @param redirectUrl
     */
    public static Authentication initialize(String sessionId, String tokenId, Instant validUntil, String externalId, URL redirectUrl) {
        return new Authentication(AuthenticationState.INITIALIZED, sessionId, tokenId, validUntil, externalId, redirectUrl);
    }

    /**
     * Factory method to restore an Authentication in INITIALIZED status.
     *
     * @param sessionId  identifies the HTTP session, must not be NULL
     * @param tokenId    identifier handed to the eID client, must not be NULL
     * @param validUntil the Authentication times out at this point in time, must not be NULL
     * @param created    the creation time, must not be NULL
     * @param externalId an id from the system which request the authentication
     */
    public static Authentication restoreInitialized(String sessionId, String tokenId,
                                                    Instant validUntil, Instant created, String externalId, URL redirectUrl) {
        var result = new Authentication(AuthenticationState.INITIALIZED, sessionId, tokenId, validUntil, externalId, redirectUrl);
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
     * @param externalId an id from the system which request the authentication
     */
    public static Authentication restoreStarted(String sessionId, String tokenId, String samlId,
                                                Instant validUntil, Instant created, String externalId, URL redirectUrl) {
        var result = new Authentication(AuthenticationState.STARTED, sessionId, tokenId, validUntil, externalId, redirectUrl);
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
     * @param validUntil  the Authentication times out at this point in time, must not be NULL
     * @param created     the creation time, must not be NULL
     * @param externalId  an id from the system which request the authentication
     */
    public static Authentication restoreResponded(String sessionId, String tokenId, String samlId,
                                                  String referenceId, Instant validUntil,
                                                  Instant created, String externalId, URL redirectUrl) {
        var result = new Authentication(AuthenticationState.RESPONDED, sessionId, tokenId, validUntil, externalId, redirectUrl);
        result.samlId = Objects.requireNonNull(samlId, SAML_ID_NOT_NULL);
        result.referenceId = Objects.requireNonNull(referenceId, REFERENCE_ID_NOT_NULL);
        result.created = Objects.requireNonNull(created, CREATED_NOT_NULL);

        return result;
    }

    /**
     * Factory method to restore an Authentication in AUTHENTICATED status.
     *
     * @param sessionId   identifies the HTTP session, must not be NULL
     * @param tokenId     identifier handed to the eID client, must not be NULL
     * @param samlId      identifier for the SAML request, must not be NULL
     * @param referenceId identifier handed to the LoggedIn page, must not be NULL
     * @param validUntil  the Authentication times out at this point in time, must not be NULL
     * @param created     the creation time, must not be NULL
     * @param externalId  an id from the system which request the authentication
     */
    public static Authentication restoreAuthenticated(String sessionId, String tokenId, String samlId,
                                                      String referenceId, Instant validUntil,
                                                      Instant created, String externalId, URL redirectUrl) {
        var result = new Authentication(AuthenticationState.AUTHENTICATED, sessionId, tokenId, validUntil, externalId, redirectUrl);
        result.samlId = Objects.requireNonNull(samlId, SAML_ID_NOT_NULL);
        result.referenceId = Objects.requireNonNull(referenceId, REFERENCE_ID_NOT_NULL);
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
     * @param externalId  an id from the system which request the authentication
     */
    public static Authentication restoreTimeout(String sessionId, String tokenId, String samlId,
                                                String referenceId, Instant validUntil, Instant created, String externalId, URL redirectUrl) {
        var result = new Authentication(AuthenticationState.TIMEOUT, sessionId, tokenId, validUntil, externalId, redirectUrl);
        result.samlId = samlId;
        result.referenceId = referenceId;
        result.created = Objects.requireNonNull(created, CREATED_NOT_NULL);
        return result;
    }

    /**
     * Factory method to restore an Authentication in TERMINATED status.
     * Not every parameter may have a value and the resulting object cannot be used
     * for service access anymore (no connected Account)
     *
     * @param sessionId   identifies the HTTP session, must not be NULL
     * @param tokenId     identifier handed to the eID client, must not be NULL
     * @param samlId      identifier for the SAML request
     * @param referenceId identifier handed to the LoggedIn page
     * @param validUntil  the Authentication times out at this point in time, must not be NULL
     * @param created     (not null)
     * @param externalId  an id from the system which request the authentication
     */
    public static Authentication restoreTerminated(String sessionId, String tokenId, String samlId,
                                                   String referenceId, Instant validUntil, Instant created, String externalId, URL redirectUrl) {
        var result = new Authentication(AuthenticationState.TERMINATED, sessionId, tokenId, validUntil, externalId, redirectUrl);
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

    public synchronized void respond(String referenceId) {
        if (this.authenticationState != AuthenticationState.STARTED) {
            throwIllegalTransitionException();
        }
        this.authenticationState = AuthenticationState.RESPONDED;
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
