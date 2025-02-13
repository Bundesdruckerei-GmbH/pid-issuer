/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core;

import de.bdr.pidi.identification.core.model.AuthenticationState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class IdentificationHousekeeping {
    private final AuthenticationStore authenticationStore;

    public IdentificationHousekeeping(AuthenticationStore authenticationStore) {
        this.authenticationStore = authenticationStore;
    }

    public void cleanupExpiredAuthentications() {
        authenticationStore.updateStateByValidUntilBeforeAndAuthenticationStateNotIn(AuthenticationState.TIMEOUT, Instant.now(), List.of(AuthenticationState.TERMINATED, AuthenticationState.TIMEOUT));
        var count = authenticationStore.deleteByAuthenticationStateIn(List.of(AuthenticationState.TERMINATED, AuthenticationState.TIMEOUT));
        log.info("Deleted {} terminated or expired authentications", count);
    }
}
