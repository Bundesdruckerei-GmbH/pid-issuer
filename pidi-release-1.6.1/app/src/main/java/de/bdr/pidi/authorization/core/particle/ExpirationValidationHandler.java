/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidGrantException;
import de.bdr.pidi.authorization.core.exception.UnauthorizedException;
import de.bdr.pidi.base.requests.SeedCredentialRequest;

import java.time.Instant;

public class ExpirationValidationHandler implements OidHandler {

    public static final String INVALID_TOKEN = "invalid_token";
    private final String scheme;

    public ExpirationValidationHandler(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public void processAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session, boolean referencesPushedAuthRequest) {
        checkRequestUriExpiration(session);
    }

    @Override
    public void processTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        checkAuthorizationCodeExpiration(session);
    }

    @Override
    public void processCredentialRequest(HttpRequest<CredentialRequest> request, WResponseBuilder response, WSession session) {
        checkAccessTokenExpiration(session);
    }

    @Override
    public void processSeedCredentialRequest(HttpRequest<SeedCredentialRequest> request, WResponseBuilder response, WSession session) {
        checkAuthorizationCodeExpiration(session);
    }

    private void checkAuthorizationCodeExpiration(WSession session) {
        var expirationTime = session.getCheckedParameterAsInstant(SessionKey.AUTHORIZATION_CODE_EXP_TIME);
        if (expirationTime.isBefore(Instant.now())) {
            throw new InvalidGrantException("Session is expired");
        }
    }

    private void checkAccessTokenExpiration(WSession session) {
        var expirationTime = session.getCheckedParameterAsInstant(SessionKey.ACCESS_TOKEN_EXP_TIME);
        if (expirationTime.isBefore(Instant.now())) {
            throw new UnauthorizedException(scheme, INVALID_TOKEN, "Access token expired");
        }
    }

    private void checkRequestUriExpiration(WSession session) {
        var expirationTime = session.getCheckedParameterAsInstant(SessionKey.REQUEST_URI_EXP_TIME);
        if (expirationTime == null) {
            throw new UnauthorizedException(scheme, INVALID_TOKEN, "Request uri expiration time not exists");
        } else if (expirationTime.isBefore(Instant.now())) {
            throw new UnauthorizedException(scheme, INVALID_TOKEN, "Request uri expired");
        }
    }
}
