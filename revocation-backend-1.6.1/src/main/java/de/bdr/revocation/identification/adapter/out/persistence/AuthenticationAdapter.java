/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.adapter.out.persistence;


import de.bdr.revocation.identification.core.exception.AuthenticationNotFoundException;
import de.bdr.revocation.identification.core.model.Authentication;
import de.bdr.revocation.identification.core.model.AuthenticationState;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static de.bdr.revocation.identification.core.model.Authentication.restoreAuthenticated;
import static de.bdr.revocation.identification.core.model.Authentication.restoreInitialized;
import static de.bdr.revocation.identification.core.model.Authentication.restoreResponded;
import static de.bdr.revocation.identification.core.model.Authentication.restoreStarted;
import static de.bdr.revocation.identification.core.model.Authentication.restoreTimeout;

@RequiredArgsConstructor
@Component
public class AuthenticationAdapter {
    private final AuthenticationRepository authenticationRepository;

    @Transactional
    public Authentication findByTokenId(String tokenId) {
        var entity = authenticationRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new AuthenticationNotFoundException("No authentication found for token id: " + tokenId));
        return toDomainAuthentication(entity);
    }

    @Transactional
    public Authentication findBySamlId(String samlId) {
        var entity = authenticationRepository.findBySamlId(samlId)
                .orElseThrow(() -> new AuthenticationNotFoundException("No authentication found for saml id: " + samlId));
        return toDomainAuthentication(entity);
    }

    @Transactional
    public Authentication findBySessionId(String sessionId) {
        var entity = authenticationRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new AuthenticationNotFoundException("No authentication found for session id: " + sessionId));
        return toDomainAuthentication(entity);
    }

    @Transactional
    public int deleteByAuthenticationStateIn(Collection<AuthenticationState> authenticationStates) {
        return authenticationRepository.deleteByAuthenticationStateIn(authenticationStates);
    }

    @Transactional
    public int updateStateByValidUntilBeforeAndAuthenticationStateNotIn(AuthenticationState newState, Instant validUntil, Collection<AuthenticationState> authenticationStates) {
        return authenticationRepository.updateStateByValidUntilBeforeAndAuthenticationStateNotIn(newState, validUntil, authenticationStates);
    }

    @Transactional
    public void createWithSessionAndToken(Authentication authentication) {
        authenticationRepository.save(new AuthenticationEntity(authentication));
    }

    @Transactional
    public void updateWithSamlIdentifiedByTokenAndFormerState(Authentication authentication, AuthenticationState formerState) {
        var newState = authentication.getAuthenticationState();
        var samlId = authentication.getSamlId();
        var tokenId = authentication.getTokenId();
        if (authenticationRepository.updateSamlIdByTokenIdAndAuthenticationState(newState, samlId, tokenId, formerState) != 1) {
            throw new AuthenticationNotFoundException(String.format("Could not update saml id %s for token id %s and state %s", samlId, tokenId, formerState));
        }
    }

    @Transactional
    public void updateWithReferenceIdIdentifiedBySamlAndFormerState(Authentication authentication, AuthenticationState formerState) {
        var newState = authentication.getAuthenticationState();
        var pseudonym = authentication.getPseudonym();
        var referenceId = authentication.getReferenceId();
        var samlId = authentication.getSamlId();
        if (authenticationRepository.updateReferenceIdBySamlIdAndAuthenticationState(newState, pseudonym, referenceId, samlId, formerState) != 1) {
            throw new AuthenticationNotFoundException(String.format("Could not update reference id %s for saml id %s and state %s", referenceId, samlId, formerState));
        }
    }

    @Transactional
    public void updateWithChangedSessionValidUntilIdentifiedByReferenceAndFormerState(Authentication authentication, String oldSessionId, AuthenticationState formerState) {
        if (authenticationRepository.updateChangedSessionValidUntilByReferenceAndFormerState(authentication.getAuthenticationState(), authentication.getSessionId(), authentication.getValidUntil(), oldSessionId, formerState, authentication.getReferenceId()) != 1) {
            throw new AuthenticationNotFoundException(String.format("Authentication to be deleted not found with session id %s, reference id %s and authentication state %s", oldSessionId, authentication.getReferenceId(), formerState.name()));
        }
    }

    @Transactional
    public void updateValidUntil(Authentication authentication) {
        var validUntil = authentication.getValidUntil();
        var sessionId = authentication.getSessionId();
        var authenticationState = authentication.getAuthenticationState();
        if (authenticationRepository.updateValidUntilBySessionIdAndAuthenticationState(validUntil, sessionId, authenticationState) != 1){
            throw new AuthenticationNotFoundException(String.format("Authentication to be extended to %s not found with sessionid %s and authentication state %s", validUntil, sessionId, authenticationState));
        }
    }

    @Transactional
    public void removeIdentifiedBySessionAndFormerState(String sessionId, AuthenticationState formerState) {
        if (authenticationRepository.deleteBySessionIdAndAuthenticationState(sessionId, formerState) != 1) {
            throw new AuthenticationNotFoundException(String.format("Authentication removal for session %s and authentication state %s not successful", sessionId, formerState));
        }
    }

    private Authentication toDomainAuthentication(@NotNull AuthenticationEntity entity) {
        var state = entity.getAuthenticationState();
        return switch (state) {
            case INITIALIZED -> restoreInitialized(
                    entity.getSessionId(),
                    entity.getTokenId(),
                    entity.getValidUntil(),
                    entity.getCreated()
            );
            case STARTED -> restoreStarted(
                    entity.getSessionId(),
                    entity.getTokenId(),
                    entity.getSamlId(),
                    entity.getValidUntil(),
                    entity.getCreated()
            );
            case RESPONDED -> restoreResponded(
                    entity.getSessionId(),
                    entity.getTokenId(),
                    entity.getSamlId(),
                    entity.getReferenceId(),
                    entity.getPseudonym(),
                    entity.getValidUntil(),
                    entity.getCreated()
            );
            case AUTHENTICATED -> restoreAuthenticated(
                    entity.getSessionId(),
                    entity.getTokenId(),
                    entity.getSamlId(),
                    entity.getReferenceId(),
                    entity.getPseudonym(),
                    entity.getValidUntil(),
                    entity.getCreated()
            );
            case TIMEOUT -> restoreTimeout(
                    entity.getSessionId(),
                    entity.getTokenId(),
                    entity.getSamlId(),
                    entity.getReferenceId(),
                    entity.getValidUntil(),
                    entity.getCreated()
            );
            case TERMINATED -> Authentication.restoreTerminated(
                    entity.getSessionId(),
                    entity.getTokenId(),
                    entity.getSamlId(),
                    entity.getReferenceId(),
                    entity.getValidUntil(),
                    entity.getCreated()
            );
        };
    }
}
