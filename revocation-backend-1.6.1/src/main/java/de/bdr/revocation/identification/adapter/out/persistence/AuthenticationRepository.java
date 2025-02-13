/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.adapter.out.persistence;

import de.bdr.revocation.identification.core.model.AuthenticationState;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AuthenticationRepository extends JpaRepository<AuthenticationEntity, Long> {

    Optional<AuthenticationEntity> findByTokenId(String tokenId);

    Optional<AuthenticationEntity> findBySamlId(String samlId);

    Optional<AuthenticationEntity> findBySessionId(String sessionId);

    @Modifying
    @Query("""
            UPDATE AuthenticationEntity \
            SET authenticationState = :newAuthenticationState, \
            samlId = :samlId \
            WHERE tokenId = :tokenId AND authenticationState = :oldAuthenticationState""")
    int updateSamlIdByTokenIdAndAuthenticationState(AuthenticationState newAuthenticationState, String samlId, String tokenId, AuthenticationState oldAuthenticationState);

    @Modifying
    @Query("""
            UPDATE AuthenticationEntity \
            SET authenticationState = :newAuthenticationState, \
            pseudonym = :pseudonym, \
            referenceId = :referenceId \
            WHERE samlId = :samlId AND authenticationState = :oldAuthenticationState""")
    int updateReferenceIdBySamlIdAndAuthenticationState(AuthenticationState newAuthenticationState, String pseudonym, String referenceId, String samlId, AuthenticationState oldAuthenticationState);

    @Modifying
    @Query("""
            UPDATE AuthenticationEntity \
            SET authenticationState = :newState \
            WHERE validUntil < :validUntil AND authenticationState NOT IN :authenticationStates""")
    int updateStateByValidUntilBeforeAndAuthenticationStateNotIn(AuthenticationState newState, Instant validUntil, Collection<AuthenticationState> authenticationStates);

    @Modifying
    @Query("""
            UPDATE AuthenticationEntity \
            SET validUntil = :validUntil \
            WHERE sessionId = :sessionId AND authenticationState = :authenticationState""")
    int updateValidUntilBySessionIdAndAuthenticationState(Instant validUntil, String sessionId, AuthenticationState authenticationState);

    int deleteByAuthenticationStateIn(Collection<AuthenticationState> authenticationStates);

    int deleteBySessionIdAndAuthenticationState(@NotNull String sessionId, @NotNull AuthenticationState authenticationState);

    @Modifying
    @Query("""
            UPDATE AuthenticationEntity \
            SET authenticationState = :authenticationState, \
            sessionId = :sessionId, \
            validUntil = :validUntil \
            WHERE sessionId = :oldSessionId AND authenticationState = :oldAuthenticationState AND referenceId = :referenceId"""
    )
    int updateChangedSessionValidUntilByReferenceAndFormerState(AuthenticationState authenticationState, String sessionId, Instant validUntil, String oldSessionId, AuthenticationState oldAuthenticationState, String referenceId);

}
