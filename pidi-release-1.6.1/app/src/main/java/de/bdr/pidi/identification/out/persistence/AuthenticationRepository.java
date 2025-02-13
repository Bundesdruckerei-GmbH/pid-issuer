/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.out.persistence;

import de.bdr.pidi.identification.core.model.AuthenticationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;

public interface AuthenticationRepository extends JpaRepository<AuthenticationEntity, Long> {

    AuthenticationEntity findByTokenId(String tokenId);

    AuthenticationEntity findBySamlId(String samlId);

    AuthenticationEntity findByReferenceId(String referenceId);

    AuthenticationEntity findBySessionId(String sessionId);

    long countByAuthenticationStateAndValidUntilAfter(AuthenticationState authenticationState, Instant validUntil);

    @Modifying
    @Query("UPDATE AuthenticationEntity " +
            "SET authenticationState = :newAuthenticationState, " +
            "samlId = :samlId " +
            "WHERE tokenId = :tokenId AND authenticationState = :oldAuthenticationState")
    int updateSamlIdByTokenIdAndAuthenticationState(AuthenticationState newAuthenticationState, String samlId, String tokenId, AuthenticationState oldAuthenticationState);

    @Modifying
    @Query("UPDATE AuthenticationEntity " +
            "SET authenticationState = :newAuthenticationState, " +
            "referenceId = :referenceId " +
            "WHERE samlId = :samlId AND authenticationState = :oldAuthenticationState")
    int updateReferenceIdBySamlIdAndAuthenticationState(AuthenticationState newAuthenticationState, String referenceId, String samlId, AuthenticationState oldAuthenticationState);

    @Modifying
    @Query("UPDATE AuthenticationEntity " +
            "SET authenticationState = :newState " +
            "WHERE validUntil < :validUntil AND authenticationState NOT IN :authenticationStates")
    int updateStateByValidUntilBeforeAndAuthenticationStateNotIn(AuthenticationState newState, Instant validUntil, Collection<AuthenticationState> authenticationStates);

    @Modifying
    @Query("UPDATE AuthenticationEntity " +
            "SET validUntil = :validUntil " +
            "WHERE sessionId = :sessionId AND authenticationState = :authenticationState")
    int updateValidUntilBySessionIdAndAuthenticationState(Instant validUntil, String sessionId, AuthenticationState authenticationState);

    @Modifying
    @Query("DELETE FROM AuthenticationEntity WHERE authenticationState IN :authenticationStates")
    int deleteByAuthenticationStateIn(Collection<AuthenticationState> authenticationStates);

    @Modifying
    @Query("DELETE FROM AuthenticationEntity WHERE sessionId = :sessionId AND referenceId = :referenceId AND authenticationState = :authenticationState")
    int deleteBySessionIdAndReferenceIdAndAuthenticationState(String sessionId, String referenceId, AuthenticationState authenticationState);

    @Modifying
    @Query("DELETE FROM AuthenticationEntity WHERE sessionId = :sessionId AND authenticationState = :authenticationState")
    int deleteBySessionIdAndAuthenticationState(String sessionId, AuthenticationState authenticationState);

    @Modifying
    @Query("UPDATE AuthenticationEntity " +
            "SET authenticationState = :authenticationState, " +
            "sessionId = :sessionId, " +
            "validUntil = :validUntil " +
            "WHERE sessionId = :oldSessionId AND authenticationState = :oldAuthenticationState AND referenceId = :referenceId"
    )
    int updateChangedSessionValidUntilByReferenceAndFormerState(AuthenticationState authenticationState, String sessionId, Instant validUntil, String oldSessionId, AuthenticationState oldAuthenticationState, String referenceId);

}
