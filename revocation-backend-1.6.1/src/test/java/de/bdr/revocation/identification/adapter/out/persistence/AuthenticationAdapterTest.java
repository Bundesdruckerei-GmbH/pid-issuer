/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.adapter.out.persistence;

import de.bdr.revocation.identification.core.model.AuthenticationState;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Builder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.bdr.revocation.identification.core.model.AuthenticationState.AUTHENTICATED;
import static de.bdr.revocation.identification.core.model.AuthenticationState.INITIALIZED;
import static de.bdr.revocation.identification.core.model.AuthenticationState.RESPONDED;
import static de.bdr.revocation.identification.core.model.AuthenticationState.STARTED;
import static de.bdr.revocation.identification.core.model.AuthenticationState.TERMINATED;
import static de.bdr.revocation.identification.core.model.AuthenticationState.TIMEOUT;
import static de.bdr.revocation.identification.core.model.ModelTestData.defaultAuthentication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationAdapterTest {

    private static Stream<Arguments> provideEntitiesWithStatus() {
        return Stream.of(
                Arguments.of(INITIALIZED, defaultAuthenticationEntity().authenticationState(INITIALIZED).build()),
                Arguments.of(STARTED, defaultAuthenticationEntity().authenticationState(STARTED).build()),
                Arguments.of(RESPONDED, defaultAuthenticationEntity().authenticationState(RESPONDED).build()),
                Arguments.of(AUTHENTICATED, defaultAuthenticationEntity().authenticationState(AUTHENTICATED).build()),
                Arguments.of(TIMEOUT, defaultAuthenticationEntity().authenticationState(TIMEOUT).build()),
                Arguments.of(TERMINATED, defaultAuthenticationEntity().authenticationState(TERMINATED).build())
        );
    }

    private final Collection<AuthenticationState> statesRelevantForHousekeeping = List.of(TIMEOUT, TERMINATED);

    @Mock
    private AuthenticationRepository repo;

    @InjectMocks
    private AuthenticationAdapter out;

    @ParameterizedTest
    @MethodSource("provideEntitiesWithStatus")
    void given_entity_findBySessionId_restores_to_correct_state(AuthenticationState state, AuthenticationEntity entity) {
        var sessionId = entity.getSessionId();
        when(repo.findBySessionId(sessionId)).thenReturn(Optional.of(entity));
        assertThat(out.findBySessionId(sessionId).getAuthenticationState()).isEqualTo(state);
    }

    @ParameterizedTest
    @MethodSource("provideEntitiesWithStatus")
    void given_entity_findBySamlId_restores_to_correct_state(AuthenticationState state, AuthenticationEntity entity) {
        var samlId = entity.getSamlId();
        when(repo.findBySamlId(samlId)).thenReturn(Optional.of(entity));
        assertThat(out.findBySamlId(samlId).getAuthenticationState()).isEqualTo(state);
    }

    @ParameterizedTest
    @MethodSource("provideEntitiesWithStatus")
    void given_entity_findByTokenId_restores_to_correct_state(AuthenticationState state, AuthenticationEntity entity) {
        var tokenId = entity.getTokenId();
        when(repo.findByTokenId(tokenId)).thenReturn(Optional.of(entity));
        assertThat(out.findByTokenId(tokenId).getAuthenticationState()).isEqualTo(state);
    }

    @Test
    void given_no_entity_found_findBySessionId_throws_authentication_exception() {
        when(repo.findBySessionId(any())).thenReturn(null);
        assertThrows(RuntimeException.class, () -> out.findBySessionId("NoneExistentSessionId"));
    }

    @Test
    void given_no_entity_found_findBySamlId_throws_authentication_exception() {
        when(repo.findBySamlId(any())).thenReturn(null);
        assertThrows(RuntimeException.class, () -> out.findBySamlId("NoneExistentSamlId"));
    }

    @Test
    void given_no_entity_found_findByTokenId_throws_authentication_exception() {
        when(repo.findByTokenId(any())).thenReturn(null);
        assertThrows(RuntimeException.class, () -> out.findByTokenId("NoneExistentTokenId"));
    }

    @Test
    void given_authentication_states_gets_correct_number_of_deleted_from_repo() {
        when(repo.deleteByAuthenticationStateIn(statesRelevantForHousekeeping)).thenReturn(10);
        assertThat(out.deleteByAuthenticationStateIn(statesRelevantForHousekeeping)).isEqualTo(10);
    }

    @Test
    void given_authentication_states_and_valid_until_gets_correct_number_of_updated_from_repo() {
        var validUntil = Instant.now();
        when(repo.updateStateByValidUntilBeforeAndAuthenticationStateNotIn(TIMEOUT, validUntil, statesRelevantForHousekeeping)).thenReturn(10);
        assertThat(out.updateStateByValidUntilBeforeAndAuthenticationStateNotIn(TIMEOUT, validUntil, statesRelevantForHousekeeping)).isEqualTo(10);
    }

    @Test
    void given_authentication_service_calls_repository_for_saving() {
        out.createWithSessionAndToken(defaultAuthentication().build());
        verify(repo, times(1)).save(any());
    }

    @Test
    void given_update_saml_returns_no_change_then_throws_exception() {
        var auth = defaultAuthentication().authenticationState(STARTED).build();
        var tokenId = auth.getTokenId();
        when(repo.updateSamlIdByTokenIdAndAuthenticationState(
                auth.getAuthenticationState(),
                auth.getSamlId(),
                tokenId,
                INITIALIZED)
        ).thenReturn(0);
        assertThrows(RuntimeException.class, () -> out.updateWithSamlIdentifiedByTokenAndFormerState(auth, INITIALIZED));
    }

    @Test
    void given_update_saml_returns_one_change_then_does_not_throw_exception() {
        var auth = defaultAuthentication().authenticationState(STARTED).build();
        var tokenId = auth.getTokenId();
        when(repo.updateSamlIdByTokenIdAndAuthenticationState(
                auth.getAuthenticationState(),
                auth.getSamlId(),
                tokenId,
                INITIALIZED)
        ).thenReturn(1);
        assertDoesNotThrow(() -> out.updateWithSamlIdentifiedByTokenAndFormerState(auth, INITIALIZED));
    }

    @Test
    void given_update_saml_returns_more_than_one_change_then_throws_exception() {
        var auth = defaultAuthentication().authenticationState(STARTED).build();
        var tokenId = auth.getTokenId();
        when(repo.updateSamlIdByTokenIdAndAuthenticationState(
                auth.getAuthenticationState(),
                auth.getSamlId(),
                tokenId,
                INITIALIZED)
        ).thenReturn(2);
        assertThrows(RuntimeException.class, () -> out.updateWithSamlIdentifiedByTokenAndFormerState(auth, INITIALIZED));
    }

    @Test
    void given_update_reference_returns_no_change_then_throws_exception() {
        var auth = defaultAuthentication().authenticationState(RESPONDED).build();
        var samlId = auth.getSamlId();
        when(repo.updateReferenceIdBySamlIdAndAuthenticationState(
                auth.getAuthenticationState(),
                auth.getPseudonym(),
                auth.getReferenceId(),
                samlId,
                STARTED)
        ).thenReturn(0);
        assertThrows(RuntimeException.class, () -> out.updateWithReferenceIdIdentifiedBySamlAndFormerState(auth, STARTED));
    }

    @Test
    void given_update_reference_returns_one_change_then_does_not_throw_exception() {
        var auth = defaultAuthentication().authenticationState(RESPONDED).build();
        var samlId = auth.getSamlId();
        when(repo.updateReferenceIdBySamlIdAndAuthenticationState(
                auth.getAuthenticationState(),
                auth.getPseudonym(),
                auth.getReferenceId(),
                samlId,
                STARTED)
        ).thenReturn(1);
        assertDoesNotThrow(() -> out.updateWithReferenceIdIdentifiedBySamlAndFormerState(auth, STARTED));
    }

    @Test
    void given_update_reference_returns_more_than_one_change_then_throws_exception() {
        var auth = defaultAuthentication().authenticationState(RESPONDED).build();
        var samlId = auth.getSamlId();
        when(repo.updateReferenceIdBySamlIdAndAuthenticationState(
                auth.getAuthenticationState(),
                auth.getPseudonym(),
                auth.getReferenceId(),
                samlId,
                STARTED)
        ).thenReturn(2);
        assertThrows(RuntimeException.class, () -> out.updateWithReferenceIdIdentifiedBySamlAndFormerState(auth, STARTED));
    }

    @Test
    void given_update_changed_session_returns_no_change_then_throws_exception() {
        var oldSessionId = "oldSessionId";
        var oldReferenceId = "oldReferenceId";
        var auth = defaultAuthentication()
                .authenticationState(AUTHENTICATED)
                .sessionId(oldSessionId)
                .referenceId(oldReferenceId)
                .build();

        when(repo.updateChangedSessionValidUntilByReferenceAndFormerState(auth.getAuthenticationState(), auth.getSessionId(), auth.getValidUntil(),
                oldSessionId, RESPONDED, oldReferenceId)
        ).thenReturn(0);
        assertThrows(RuntimeException.class, () -> out.updateWithChangedSessionValidUntilIdentifiedByReferenceAndFormerState(auth, oldSessionId, RESPONDED));
    }

    @Test
    void given_update_changed_session_returns_one_change_then_does_not_throw_exception() {
        var oldSessionId = "oldSessionId";
        var oldReferenceId = "oldReferenceId";
        var auth = defaultAuthentication()
                .authenticationState(AUTHENTICATED)
                .sessionId(oldSessionId)
                .referenceId(oldReferenceId)
                .build();

        when(repo.updateChangedSessionValidUntilByReferenceAndFormerState(
                auth.getAuthenticationState(), auth.getSessionId(), auth.getValidUntil(),
                oldSessionId, RESPONDED, oldReferenceId)
        ).thenReturn(1);
        assertDoesNotThrow(() -> out.updateWithChangedSessionValidUntilIdentifiedByReferenceAndFormerState(auth, oldSessionId, RESPONDED));
    }

    @Test
    void given_update_changed_session_returns_more_than_one_change_then_throws_exception() {

        var oldSessionId = "oldSessionId";
        var oldReferenceId = "oldReferenceId";
        var auth = defaultAuthentication()
                .authenticationState(AUTHENTICATED)
                .referenceId(oldReferenceId)
                .sessionId(oldSessionId)
                .build();
        when(repo.updateChangedSessionValidUntilByReferenceAndFormerState(
                AUTHENTICATED, auth.getSessionId(), auth.getValidUntil(),
                oldSessionId, RESPONDED, oldReferenceId)
        ).thenReturn(2);
        assertThrows(RuntimeException.class, () -> out.updateWithChangedSessionValidUntilIdentifiedByReferenceAndFormerState(auth, oldSessionId, RESPONDED));
    }

    @Test
    void given_remove_identified_by_session_and_state_returns_no_delete_then_throws_exception() {
        var auth = defaultAuthentication().authenticationState(TERMINATED).build();
        var sessionId = auth.getSessionId();
        when(repo.deleteBySessionIdAndAuthenticationState(sessionId, AUTHENTICATED)).thenReturn(0);
        assertThrows(RuntimeException.class, () -> out.removeIdentifiedBySessionAndFormerState(sessionId, AUTHENTICATED));
    }

    @Test
    void given_remove_identified_by_session_and_state_returns_one_delete_then_does_not_throw_exception() {
        var auth = defaultAuthentication().authenticationState(TERMINATED).build();
        var sessionId = auth.getSessionId();
        when(repo.deleteBySessionIdAndAuthenticationState(sessionId, AUTHENTICATED)).thenReturn(1);
        assertDoesNotThrow(() -> out.removeIdentifiedBySessionAndFormerState(sessionId, AUTHENTICATED));
    }

    @Test
    void given_remove_identified_by_session_and_state_returns_more_than_one_delete_then_throws_exception() {
        var auth = defaultAuthentication().authenticationState(TERMINATED).build();
        var sessionId = auth.getSessionId();
        when(repo.deleteBySessionIdAndAuthenticationState(sessionId, AUTHENTICATED)).thenReturn(2);
        assertThrows(RuntimeException.class, () -> out.removeIdentifiedBySessionAndFormerState(sessionId, AUTHENTICATED));
    }

    @Test
    void given_authentication_then_updates_valid_until() {
        var validUntil = Instant.now().plusSeconds(300);
        var sessionId = "MyTestId";
        var auth = defaultAuthentication()
                .authenticationState(AUTHENTICATED)
                .validUntil(validUntil)
                .sessionId(sessionId).build();
        when(repo.updateValidUntilBySessionIdAndAuthenticationState(validUntil, sessionId, AUTHENTICATED)).thenReturn(1);
        out.updateValidUntil(auth);
        verify(repo).updateValidUntilBySessionIdAndAuthenticationState(validUntil, sessionId, AUTHENTICATED);
    }

    @Test
    void given_non_existent_authentication_then_exception() {
        var validUntil = Instant.now().plusSeconds(300);
        var sessionId = "MyTestId";
        var auth = defaultAuthentication()
                .authenticationState(AUTHENTICATED)
                .validUntil(validUntil)
                .sessionId(sessionId).build();
        when(repo.updateValidUntilBySessionIdAndAuthenticationState(validUntil, sessionId, AUTHENTICATED)).thenReturn(0);
        assertThrows(RuntimeException.class, () -> out.updateValidUntil(auth));
        verify(repo).updateValidUntilBySessionIdAndAuthenticationState(validUntil, sessionId, AUTHENTICATED);
    }

    @Builder(builderMethodName = "authenticationEntityBuilder")
    public static AuthenticationEntity buildAuthenticationEntity(Long id, AuthenticationState authenticationState, String sessionId, String samlId, String tokenId, String referenceId, String pseudonym, Instant validUntil, Instant created) {
        AuthenticationEntity entity = new AuthenticationEntity();
        entity.setId(id);
        entity.setAuthenticationState(authenticationState);
        entity.setSessionId(sessionId);
        entity.setSamlId(samlId);
        entity.setTokenId(tokenId);
        entity.setReferenceId(referenceId);
        entity.setPseudonym(pseudonym);
        entity.setValidUntil(validUntil);
        entity.setCreated(created);
        return entity;
    }

    public static AuthenticationEntityBuilder defaultAuthenticationEntity() {
        var now = Instant.now();
        return authenticationEntityBuilder()
                .authenticationState(AuthenticationState.INITIALIZED)
                .sessionId("TestSessionId")
                .samlId("TestSamlId")
                .tokenId("TestTokenId")
                .referenceId("TestReferenceId")
                .pseudonym("TestPseudonym")
                .created(now)
                .validUntil(now.plus(1, ChronoUnit.DAYS));
    }
}