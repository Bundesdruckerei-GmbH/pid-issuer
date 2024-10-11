/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidGrantException;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.exception.ValidationFailedException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;

public class RedirectUriHandler implements OidHandler {
    private static final String REDIRECT_URI = "redirect_uri";
    public static final String INVALID_REDIRECT_URI = "Invalid redirect URI";

    @Override
    public void processPushedAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        String redirectUri = validateRedirectUriParameter(request.getParameters());
        session.putParameter(SessionKey.REDIRECT_URI, redirectUri);
    }

    @Override
    public void processTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        String redirectUriFromRequest = request.getParameters().get(REDIRECT_URI);
        if (redirectUriFromRequest == null || redirectUriFromRequest.isBlank()) {
            throw new ValidationFailedException(INVALID_REDIRECT_URI, "redirect_uri must not be empty");
        }
        String redirectUriFromSession = session.getParameter(SessionKey.REDIRECT_URI);
        if (!Objects.equals(redirectUriFromRequest, redirectUriFromSession)) {
            throw new InvalidGrantException(INVALID_REDIRECT_URI);
        }
    }

    private String validateRedirectUriParameter(Map<String, String> parameters) {
        if (!parameters.containsKey(REDIRECT_URI)) {
            throw new InvalidRequestException(InvalidRequestException.missingParameter(REDIRECT_URI));
        }
        String redirectUri = parameters.get(REDIRECT_URI);

        if (redirectUri == null || redirectUri.isEmpty()) {
            throw new ValidationFailedException(INVALID_REDIRECT_URI, "redirect_uri must not be empty");
        }
        try {
            URI uri = new URI(redirectUri);
            if (!"https".equals(uri.getScheme())) {
                throw new ValidationFailedException(INVALID_REDIRECT_URI, "redirect_uri must start with https");
            }
        } catch (URISyntaxException e) {
            throw new ValidationFailedException(INVALID_REDIRECT_URI, "redirect_uri must be a valid URI");
        }
        return redirectUri;
    }
}
