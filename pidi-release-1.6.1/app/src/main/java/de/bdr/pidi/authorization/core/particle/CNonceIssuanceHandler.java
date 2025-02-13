/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.NonceFactory;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.Nonce;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@RequiredArgsConstructor
public class CNonceIssuanceHandler implements OidHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Duration accessTokenLifetime;

    @Override
    public void processTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        issueCNonce(response, session);
    }

    @Override
    public void processRefreshTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        issueCNonce(response, session);
    }

    @Override
    public void processSeedCredentialTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        issueCNonce(response, session);
    }

    @Override
    public void processCredentialRequest(HttpRequest<CredentialRequest> request, WResponseBuilder response, WSession session) {
        issueCNonce(response, session);
    }

    private void issueCNonce(WResponseBuilder response, WSession session) {
        Nonce nonce = NonceFactory.createSecureRandomNonce(accessTokenLifetime);

        var body = objectMapper.createObjectNode()
                .put("c_nonce", nonce.nonce())
                .put("c_nonce_expires_in", nonce.expiresIn().toSeconds());
        response.withJsonBody(body);

        session.putParameter(SessionKey.C_NONCE, nonce.nonce());
        session.putParameter(SessionKey.C_NONCE_EXP_TIME, nonce.expirationTime());
    }
}
