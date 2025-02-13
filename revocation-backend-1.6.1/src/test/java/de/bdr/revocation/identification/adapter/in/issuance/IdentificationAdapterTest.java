/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.adapter.in.issuance;

import de.bdr.revocation.identification.core.AuthenticationService;
import de.bdr.revocation.identification.core.IdentificationException;
import de.bdr.revocation.identification.core.model.Authentication;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class IdentificationAdapterTest {
    @Mock
    private AuthenticationService service;

    @InjectMocks
    private IdentificationAdapter identificationAdapter;

    @Test
    void shouldPseudonymWhenSessionIdValid() {
        // given
        String sessionId = UUID.randomUUID().toString();
        String pseudonym = "pseudo";
        doReturn(newAuthentication(sessionId, pseudonym)).when(service).retrieveAuth(sessionId, null);
        // when
        Optional<String> pseudonymOptional = identificationAdapter.validateSessionAndGetPseudonym(sessionId);
        // then
        assertThat(pseudonymOptional).contains(pseudonym);
    }

    @Test
    void shouldEmptyWhenSessionIdInvalid() {
        // given
        String sessionId = UUID.randomUUID().toString();
        doThrow(new IdentificationException("Invalid")).when(service).retrieveAuth(sessionId, null);
        // when
        Optional<String> pseudonymOptional = identificationAdapter.validateSessionAndGetPseudonym(sessionId);
        // then
        assertThat(pseudonymOptional).isEmpty();
    }

    private Authentication newAuthentication(String sessionID, String pseudonym) {
        Instant now = Instant.now();
        val uuid = UUID.randomUUID().toString();
        return Authentication.restoreAuthenticated(sessionID, uuid, uuid, uuid, pseudonym, now.plusSeconds(600L), now);
    }
}