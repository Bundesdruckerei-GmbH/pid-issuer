/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.adapter.in.issuance;

import de.bdr.revocation.identification.core.AuthenticationService;
import de.bdr.revocation.identification.core.IdentificationException;
import de.bdr.revocation.identification.core.model.Authentication;
import de.bdr.revocation.issuance.adapter.out.identification.IdentificationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class IdentificationAdapter implements IdentificationApi {
    private final AuthenticationService authenticationService;

    @Override
    public Optional<String> validateSessionAndGetPseudonym(String sessionId) {
        try {
            Authentication authentication = authenticationService.retrieveAuth(sessionId, null);
            return Optional.ofNullable(authentication.getPseudonym());
        } catch (IdentificationException e) {
            return Optional.empty();
        }
    }
}
