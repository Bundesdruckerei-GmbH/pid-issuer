/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.exception.ValidationFailedException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.bdr.pidi.authorization.core.exception.InvalidRequestException.missingParameter;

public class ScopeHandler implements OidHandler {
    private static final String SCOPE = "scope";
    private final Collection<String> validScopes;

    public ScopeHandler(FlowVariant flowVariant) {
        validScopes = switch (flowVariant) {
            case C, C1, C2, B, B1 -> List.of("pid");
            default -> List.of();
        };
    }

    @Override
    public void processPushedAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        validateAndGetScopeParameter(request.getParameters(), true)
                .ifPresent(scope -> session.putParameter(SessionKey.SCOPE, scope));
    }

    @Override
    public void processRefreshTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        validateAndGetScopeParameter(request.getParameters(), false)
                .ifPresent(scope -> session.putParameter(SessionKey.SCOPE, scope));
    }

    private Optional<String> validateAndGetScopeParameter(Map<String, String> params, boolean scopeIsRequired) {
        if (params.containsKey(SCOPE)) {
            String scope = params.get(SCOPE);
            if (scope == null || scope.isEmpty()) {
                throw new ValidationFailedException("Invalid scope", "scope must not be empty");
            }
            if (!validScopes.contains(scope)) {
                throw new InvalidScopeException("Scopes %s not granted".formatted(scope));
            }
            return Optional.of(scope);
        } else if (scopeIsRequired) {
            throw new InvalidRequestException(missingParameter(SCOPE));
        }
        return Optional.empty();
    }
}
