/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.openid4vc.vci.service.pkce.Pkce;
import de.bdr.openid4vc.vci.service.pkce.PkceCodeChallengeMethod;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.ValidationFailedException;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static de.bdr.pidi.authorization.core.exception.InvalidRequestException.missingParameter;

public class PkceHandler implements OidHandler {

    /**
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-2.3">Definition of unreserved characters</a>
     */
    private static final Pattern UNRESERVED_CHAR_PATTERN = Pattern.compile("[A-Za-z0-9\\-._~]{43,128}");

    private static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
    private static final String CODE_CHALLENGE = "code_challenge";
    private static final String CODE_VERIFIER = "code_verifier";
    private static final String SHA256_METHOD = "S256";

    private static final Pkce PKCE = Pkce.INSTANCE;

    @Override
    public void processPushedAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        validateAuthorizationParams(request.getParameters());
        saveAuthorizationParamsToSession(request.getParameters(), session);
    }

    @Override
    public void processAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session, boolean referencesPushedAuthRequest)  {
        if (!referencesPushedAuthRequest) {
            validateAuthorizationParams(request.getParameters());
            saveAuthorizationParamsToSession(request.getParameters(), session);
        }
    }

    @Override
    public void processTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        if (!request.getParameters().containsKey(CODE_VERIFIER)) {
            throw new ValidationFailedException(missingParameter( CODE_VERIFIER));
        }
        String codeVerifier = request.getParameters().get(CODE_VERIFIER);
        if (codeVerifier == null || !UNRESERVED_CHAR_PATTERN.matcher(codeVerifier).matches()) {
            throw new ValidationFailedException("Invalid code verifier");
        }
        if (!PKCE.validate(session.getParameter(SessionKey.CODE_CHALLENGE), codeVerifier, PkceCodeChallengeMethod.S256)) {
            throw new VerificationFailedException("Invalid code verifier");
        }
    }

    private void validateAuthorizationParams(Map<String, String> params) {
        if (!params.containsKey(CODE_CHALLENGE_METHOD)) {
            throw new ValidationFailedException(missingParameter( CODE_CHALLENGE_METHOD));
        }
        if (!params.containsKey(CODE_CHALLENGE)) {
            throw new ValidationFailedException(missingParameter( CODE_CHALLENGE));
        }
        String method = params.get(CODE_CHALLENGE_METHOD);
        if (!Objects.equals(SHA256_METHOD, method)) {
            throw new ValidationFailedException("Invalid code challenge method");
        }
        String challenge = params.get(CODE_CHALLENGE);
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(challenge);
            if (bytes.length != 32) {
                throw new ValidationFailedException("Invalid code challenge", "Invalid code challenge length");
            }
        } catch (IllegalArgumentException e) {
            throw new ValidationFailedException("Invalid code challenge", "Invalid code challenge base64");
        }
    }

    private void saveAuthorizationParamsToSession(Map<String, String> params, WSession session) {
        session.putParameter(SessionKey.CODE_CHALLENGE_METHOD, params.get(CODE_CHALLENGE_METHOD));
        session.putParameter(SessionKey.CODE_CHALLENGE, params.get(CODE_CHALLENGE));
    }
}
