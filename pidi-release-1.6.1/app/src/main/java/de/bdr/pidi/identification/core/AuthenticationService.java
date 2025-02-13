/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core;

import de.bdr.pidi.identification.config.IdentificationConfiguration;
import de.bdr.pidi.identification.core.exception.AuthenticationNotFoundException;
import de.bdr.pidi.identification.core.exception.AuthenticationStateException;
import de.bdr.pidi.identification.core.model.Authentication;
import de.bdr.pidi.identification.core.model.AuthenticationState;
import de.bdr.pidi.identification.core.model.ResponseData;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

import static de.bdr.pidi.identification.core.model.AuthenticationState.AUTHENTICATED;
import static de.bdr.pidi.identification.core.model.AuthenticationState.INITIALIZED;
import static de.bdr.pidi.identification.core.model.AuthenticationState.RESPONDED;
import static de.bdr.pidi.identification.core.model.AuthenticationState.STARTED;
import static de.bdr.pidi.identification.core.model.AuthenticationState.TIMEOUT;

@Slf4j
@Service
public class AuthenticationService {

    private final EidAuth autent;
    private final AuthenticationStore store;
    private final RandomProvider randomProvider;
    private final IdentificationConfiguration identificationConfiguration;

    public AuthenticationService(EidAuth autent, AuthenticationStore store, IdentificationConfiguration identificationConfiguration,
                                 RandomProvider randomProvider) {
        this.autent = autent;
        this.store = store;
        this.identificationConfiguration = identificationConfiguration;
        this.randomProvider = randomProvider;
    }

    public Authentication initializeAuthentication(String externalId, URL redirectUrl, String sessionId) {
        final String authSessionId = Objects.requireNonNullElseGet(sessionId, () -> generateRandomId(getSessionRng()));
        log.debug("before Auth init");
        var until = Instant.now().plus(identificationConfiguration.getInitialSessionDuration());
        var result = Authentication.initialize(authSessionId, authSessionId, until, externalId, redirectUrl);
        this.store.createWithSessionAndToken(result);
        return result;
    }

    @Timed
    public Authentication loadBySamlId(String relayState){
       return  store.findBySamlId(relayState);
    }
    private Random getSessionRng() {
        return randomProvider.getSessionRng();
    }

    private Random getSamlRng() {
        return randomProvider.getSamlRng();
    }

    public String createSamlRedirectBindingUrl(String tokenId, String responseUrl) {
        if (tokenId == null) {
            throw new IllegalArgumentException("tokenId must not be null");
        }
        var authentication = store.findByTokenId(tokenId);
        validateAuthenticationBeforeSamlRequest(authentication);
        String samlId = generateRandomId(getSamlRng());
        var result = autent.createSamlRedirectBindingUrl(samlId, responseUrl);
        authentication.start(samlId);
        store.updateWithSamlIdentifiedByTokenAndFormerState(authentication, INITIALIZED);
        return result;
    }

    private void validateAuthenticationBeforeSamlRequest(Authentication authentication) {
        if (authentication == null) {
            throw new AuthenticationNotFoundException("no authentication found for tokenId");
        }
        validateAuthenticationState(authentication, INITIALIZED);
        validateAuthenticationIsNotTimedout(authentication);
    }

    /**
     * receive the SAML response, validate and decode it and create the <code>referenceId</code>.
     * If successful, this advances the Authentication's state.
     *
     * @param relayState   the SAML RelayState parameter
     * @param samlResponse the SAML Response
     * @param sigAlg       the Signature algorithm
     * @param signature    the Signature
     * @param responseUrl  the intended receiver of the SAML Response (used for validation)
     * @return the referenceId
     */
    @Timed
    public ResponseData receiveSamlResponse(String relayState, String samlResponse, String sigAlg, String signature, String responseUrl) {
        // I'd like to use the ID/InResponseTo elements but cannot access them here
        var authentication = store.findBySamlId(relayState);

        validateAuthenticationBeforeSamlResponse(authentication);

        ResponseData responseData = autent
                .validateSamlResponseAndExtractPseudonym(relayState, samlResponse, sigAlg, signature, responseUrl, authentication);
        if (identificationConfiguration.isLoggingPseudonymsAllowed()) {
            log.debug("received {}", responseData);
        }

        var refId = authentication.getSessionId();
        // identification ends here
        authentication.respond(refId);
        store.updateWithReferenceIdIdentifiedBySamlAndFormerState(authentication, STARTED);

        return responseData;
    }

    private void validateAuthenticationBeforeSamlResponse(Authentication authentication) {
        if (authentication == null) {
            throw new AuthenticationNotFoundException("no authentication found for SAML RelayState");
        }
        validateAuthenticationState(authentication, STARTED);
        validateAuthenticationIsNotTimedout(authentication);
    }

    /**
     * retrieve the Authentication belonging to the sessionId
     *
     * @param sessionId   one key to the Authentication
     * @param referenceId the key to finalize the Authentication,
     *                    needed to bind the TLS sessions when it is not yet AUTHENTICATED
     * @return a valid Authentication, throws Exceptions otherwise
     */
    public Authentication retrieveAuth(String sessionId, String referenceId) {
        validateSessionIdExists(sessionId);
        var authentication = store.findBySessionId(sessionId);

        validateAuthenticationAfterRetrieval(authentication, referenceId);
        return authentication;
    }

    /**
     * retrieve the Authentication belonging to the referenceId
     * for use in the Reflector (needs to be in state RESPONDED).
     *
     * @param referenceId the key to find the Authentication
     * @return a valid Authentication, throws Exceptions otherwise
     */
    public Authentication retrieveRespondedAuth(String referenceId) {
        if (referenceId == null) {
            throw new AuthenticationNotFoundException("no referenceId given");
        }
        var authentication = store.findByReferenceId(referenceId);
        validateAuthenticationAfterRetrieval(authentication, referenceId);
        return authentication;
    }

    private void validateAuthenticationAfterRetrieval(Authentication authentication, String referenceId) {
        if (authentication == null) {
            throw new AuthenticationNotFoundException("no authentication found for sessionId");
        }
        if (referenceId == null) {
            validateAuthenticationState(authentication, AUTHENTICATED);
        } else {
            validateAuthenticationState(authentication, RESPONDED);
            if (!referenceId.equals(authentication.getReferenceId())) {
                throw new AuthenticationStateException("referenceId does not match ");
            }
        }
        validateAuthenticationIsNotTimedout(authentication);
    }

    public void finishAuthentication(Authentication authentication) {
        String newSessionId = generateRandomId(getSessionRng());
        String oldSessionId = authentication.getSessionId();
        Instant validUntil = Instant.now().plus(identificationConfiguration.getMinAuthenticatedSessionDuration());
        authentication.authenticate(newSessionId, validUntil);

        store.updateWithChangedSessionValidUntilIdentifiedByReferenceAndFormerState(authentication, oldSessionId, RESPONDED);
    }

    /**
     * retrieve the Authentication belonging to the sessionId
     *
     * @param sessionId the key to the Authentication
     * @return a valid Authentication, throws Exceptions otherwise
     */
    public Authentication terminateAuthentication(String sessionId) {
        validateSessionIdExists(sessionId);
        var authentication = store.findBySessionId(sessionId);
        if (authentication == null) {
            throw new AuthenticationNotFoundException("no authentication found for sessionId");
        }
        validateAuthenticationIsNotTimedout(authentication, Duration.ofSeconds(2));
        validateAuthenticationState(authentication, AUTHENTICATED);
        var formerState = authentication.getAuthenticationState();
        authentication.terminate();
        store.removeIdentifiedBySessionAndFormerState(authentication, sessionId, formerState);
        return authentication;
    }

    private void validateSessionIdExists(String sessionId) {
        if (sessionId == null) {
            throw new AuthenticationNotFoundException("no sessionId given");
        }
    }

    private void validateAuthenticationIsNotTimedout(Authentication authentication) {
        validateAuthenticationIsNotTimedout(authentication, Duration.ofSeconds(0));
    }

    private void validateAuthenticationIsNotTimedout(Authentication authentication, TemporalAmount gracePeriod) {
        if (authentication.getAuthenticationState() == TIMEOUT || Instant.now().plus(gracePeriod).isAfter(authentication.getValidUntil())) {
            throw new AuthenticationStateException("Authentication is timed out - it was valid until: " + authentication.getValidUntil());
        }
    }

    private String generateRandomId(Random random) {
        var buffer = new byte[Authentication.ID_BYTES];
        random.nextBytes(buffer);
        var result = Hex.encodeHexString(buffer);
        Arrays.fill(buffer, (byte) 0);
        return result;
    }

    private void validateAuthenticationState(Authentication authentication, AuthenticationState expectedState) {
        var actualState = authentication.getAuthenticationState();
        if (actualState != expectedState) {
            throw new AuthenticationStateException(String.format("bad authentication state - expected: %s, got: %s", expectedState.name(), actualState.name()));
        }
    }
}
