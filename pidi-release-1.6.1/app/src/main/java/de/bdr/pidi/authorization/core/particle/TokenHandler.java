/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
public class TokenHandler implements OidHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Duration accessTokenLifetime;
    private final String scheme;

    @Override
    public void processTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        // Validation not neccessary:
        // check grant_type not needed here - is done by AuthenticationDelegate
        // check code, refresh_token not needed here - is done by SessionManager
        // check redirect_uri not needed here - is done by RedirectUriHandler
        // check dpop-nonce not needed here - is done by DpopValidationHandler
        createAndSaveAccessToken(response, session);
    }

    @Override
    public void processRefreshTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        createAndSaveAccessToken(response, session);
    }

    @Override
    public void processSeedCredentialTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        createAndSaveAccessToken(response, session);
    }

    private void createAndSaveAccessToken(WResponseBuilder response, WSession session) {
        var token = RandomUtil.randomString();
        var tokenExpirationTime = Instant.now().plus(accessTokenLifetime);
        session.putParameter(SessionKey.ACCESS_TOKEN, token);
        session.putParameter(SessionKey.ACCESS_TOKEN_EXP_TIME, tokenExpirationTime);

        ObjectNode body = objectMapper.createObjectNode()
                .put("access_token", token)
                .put("token_type", scheme)
                .put("expires_in", accessTokenLifetime.toSeconds());
        response.withJsonBody(body);
    }
}
