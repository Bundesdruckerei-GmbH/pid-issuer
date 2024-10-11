/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidGrantException;
import de.bdr.pidi.authorization.core.service.PidSerializer;
import de.bdr.pidi.authorization.core.util.RequestUtil;
import de.bdr.pidi.authorization.out.issuance.SeedException;
import de.bdr.pidi.authorization.out.issuance.SeedPidBuilder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RefreshTokenValidationHandler implements OidHandler {
    public static final String REFRESH_TOKEN = "refresh_token";

    private final SeedPidBuilder seedPidBuilder;
    private final PidSerializer pidSerializer;
    private final String credentialIssuerIdentifier;

    @Override
    public void processRefreshTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        var refreshToken = RequestUtil.getParam(request, REFRESH_TOKEN);
        final SeedPidBuilder.EncSeedData seedData;
        try {
            seedData = seedPidBuilder.extractVerifiedEncSeed(refreshToken, credentialIssuerIdentifier);
        } catch (SeedException e) {
            throw new InvalidGrantException("Refresh token invalid", e);
        }
        JWK dpopJwk = session.getCheckedParameterAsJwk(SessionKey.DPOP_PUBLIC_KEY);
        if (!seedData.holderBindingKey().equals(dpopJwk)) {
            throw new InvalidGrantException("Key mismatch");
        }
        session.putParameter(SessionKey.IDENTIFICATION_RESULT, "Success");
        session.putParameter(SessionKey.IDENTIFICATION_DATA, pidSerializer.toString(seedData.pidCredentialData()));
    }
}
