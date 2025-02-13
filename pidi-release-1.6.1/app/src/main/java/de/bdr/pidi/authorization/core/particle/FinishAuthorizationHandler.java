/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.exception.ValidationFailedException;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import io.micrometer.common.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import static de.bdr.pidi.authorization.core.exception.InvalidRequestException.missingParameter;

public class FinishAuthorizationHandler implements OidHandler {
    private static final String ISSUER_STATE = "issuer_state";
    private static final String CODE = "code";
    private static final String STATE = "state";

    private final Duration authorizationCodeLifetime;

    public FinishAuthorizationHandler(Duration authorizationCodeLifetime) {
        this.authorizationCodeLifetime = authorizationCodeLifetime;
    }

    @Override
    public void processFinishAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        validateIssuerState(request, session);
        var redirectUri = session.getParameter(SessionKey.REDIRECT_URI);
        if (StringUtils.isBlank(redirectUri)) {
            throw new InvalidRequestException("Missing redirect uri");
        }
        validateIdentificationResult(session);

        var code = RandomUtil.randomString();
        session.putParameter(SessionKey.AUTHORIZATION_CODE, code);

        var redirectUriBuilder = UriComponentsBuilder.fromUriString(redirectUri).queryParam(CODE, code);
        if (session.containsParameter(SessionKey.STATE)) {
            redirectUriBuilder.queryParam(STATE, session.getParameter(SessionKey.STATE));
        }

        var authorizationCodeExpirationTime = Instant.now().plus(authorizationCodeLifetime);
        session.putParameter(SessionKey.AUTHORIZATION_CODE_EXP_TIME, authorizationCodeExpirationTime);

        response.withHttpStatus(302)
                .addStringHeader("Location", redirectUriBuilder.toUriString());
    }

    private static void validateIssuerState(HttpRequest<?> request, WSession session) {
        var issuerState = request.getParameters().get(ISSUER_STATE);
        if (StringUtils.isBlank(issuerState)) {
            throw new ValidationFailedException(missingParameter(ISSUER_STATE));
        } else if (!RandomUtil.isValid(issuerState)) {
            throw new ValidationFailedException("Invalid issuer state", "Invalid issuer state " + issuerState);
        } else {
            var issuerStateFromSession = session.getParameter(SessionKey.ISSUER_STATE);
            if (!Objects.equals(issuerState, issuerStateFromSession)) {
                throw new VerificationFailedException("Invalid issuer state");
            }
        }
    }

    private static void validateIdentificationResult(WSession session) {
        switch (session.getParameter(SessionKey.IDENTIFICATION_RESULT)) {
            case "Success":
                break;
            case "Error":
                var description = session.getParameter(SessionKey.IDENTIFICATION_ERROR);
                throw new IdentificationFailedException(description);
            case null:
            default:
                throw new IdentificationFailedException("neither Success nor Error result received yet");
        }
    }
}
