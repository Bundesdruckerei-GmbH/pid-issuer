/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core;

import de.bdr.revocation.identification.adapter.out.persistence.AuthenticationAdapter;
import de.bdr.revocation.identification.config.IdentificationConfiguration;
import de.bdr.revocation.identification.core.exception.AuthenticationNotFoundException;
import de.bdr.revocation.identification.core.exception.AuthenticationStateException;
import de.bdr.revocation.identification.core.model.Authentication;
import de.bdr.revocation.identification.core.model.AuthenticationState;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import static de.bdr.revocation.identification.core.model.AuthenticationState.AUTHENTICATED;
import static de.bdr.revocation.identification.core.model.AuthenticationState.INITIALIZED;
import static de.bdr.revocation.identification.core.model.AuthenticationState.RESPONDED;
import static de.bdr.revocation.identification.core.model.AuthenticationState.STARTED;
import static de.bdr.revocation.identification.core.model.AuthenticationState.TIMEOUT;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthenticationService {

    private final EidAuth autent;
    private final AuthenticationAdapter authenticationAdapter;
    private final RandomProviderSoftwareImpl randomProvider;

    private final IdentificationConfiguration identificationConfiguration;


    public Authentication initializeAuthentication() {
        var sessionId = generateRandomId(getSessionRng());
        var tokenId = generateRandomId(getTokenRng());
        log.debug("before Auth init");
        var until = Instant.now().plus(identificationConfiguration.getInitialSessionDuration());
        var result = Authentication.initialize(sessionId, tokenId, until);
        this.authenticationAdapter.createWithSessionAndToken(result);
        return result;
    }

    private Random getSessionRng() {
        return randomProvider.getSessionRng();
    }

    private Random getTokenRng() {
        return randomProvider.getTokenRng();
    }

    private Random getSamlRng() {
        return randomProvider.getSamlRng();
    }

    public String createSamlRedirectBindingUrl(String tokenId) {
        if (tokenId == null) {
            throw new IllegalArgumentException("tokenId must not be null");
        }
        var authentication = authenticationAdapter.findByTokenId(tokenId);
        validateAuthenticationBeforeSamlRequest(authentication);
        String samlId = generateRandomId(getSamlRng());
        var result = autent.createSamlRedirectBindingUrl(samlId);
        authentication.start(samlId);
        authenticationAdapter.updateWithSamlIdentifiedByTokenAndFormerState(authentication, INITIALIZED);
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
     * @param sigAlg  the Signature algorithm
     * @param signature  the Signature
     * @return the referenceId
     */
    public String receiveSamlResponse(String relayState, String samlResponse, String sigAlg, String signature) {
        // I'd like to use the ID/InResponseTo elements but cannot access them here
        var authentication = authenticationAdapter.findBySamlId(relayState);
        validateAuthenticationBeforeSamlResponse(authentication);

        var result = autent
                .validateSamlResponseAndExtractPseudonym(relayState, samlResponse, sigAlg, signature, authentication);
        if (identificationConfiguration.isLoggingPseudonymsAllowed()) {
            log.debug("pseudonym for test purposes: {}", result.pseudonym());
        }

        var refId = generateRandomId(getTokenRng());

        authentication.respond(result.pseudonym(), refId);
        authenticationAdapter.updateWithReferenceIdIdentifiedBySamlAndFormerState(authentication, STARTED);

        return refId;
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
        var authentication = authenticationAdapter.findBySessionId(sessionId);
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

        authenticationAdapter.updateWithChangedSessionValidUntilIdentifiedByReferenceAndFormerState(authentication, oldSessionId, RESPONDED);
    }

    public void extendAuthenticationValidity(Authentication authentication) {

        validateAuthenticationState(authentication, AUTHENTICATED);
        var maxValidUntil = authentication.getCreated().plus(identificationConfiguration.getMaxAuthenticatedSessionDuration());
        var newValidUntil = Instant.now().plus(identificationConfiguration.getMinAuthenticatedSessionDuration());
        authentication.extendTo(newValidUntil.isAfter(maxValidUntil) ? maxValidUntil : newValidUntil);
        authenticationAdapter.updateValidUntil(authentication);
    }

    /**
     * retrieve the Authentication belonging to the sessionId
     *
     * @param sessionId the key to the Authentication
     */
    public void terminateAuthentication(String sessionId) {
        validateSessionIdExists(sessionId);
        var authentication = authenticationAdapter.findBySessionId(sessionId);
        if (authentication == null) {
            throw new AuthenticationNotFoundException("no authentication found for sessionId");
        }
        validateAuthenticationIsNotTimedout(authentication, Duration.ofSeconds(2));
        validateAuthenticationState(authentication, AUTHENTICATED);
        var formerState = authentication.getAuthenticationState();
        authentication.terminate();
        authenticationAdapter.removeIdentifiedBySessionAndFormerState(sessionId, formerState);
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
        return Hex.encodeHexString(buffer);
    }

    private void validateAuthenticationState(Authentication authentication, AuthenticationState expectedState) {
        var actualState = authentication.getAuthenticationState();
        if (actualState != expectedState) {
            throw new AuthenticationStateException(String.format("bad authentication state - expected: %s, got: %s", expectedState.name(), actualState.name()));
        }
    }

}
