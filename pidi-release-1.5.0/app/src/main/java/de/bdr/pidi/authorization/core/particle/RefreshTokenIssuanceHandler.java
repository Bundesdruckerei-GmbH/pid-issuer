/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.service.PidSerializer;
import de.bdr.pidi.authorization.out.issuance.SeedPidBuilder;

public class RefreshTokenIssuanceHandler implements OidHandler {
    // Response keys
    public static final String REFRESH_TOKEN = "refresh_token";

    private final SeedPidBuilder seedPidBuilder;
    private final PidSerializer pidSerializer;
    private final String credentialIssuerIdentifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RefreshTokenIssuanceHandler(SeedPidBuilder seedPidBuilder, PidSerializer pidSerializer, String credentialIssuerIdentifier) {
        this.seedPidBuilder = seedPidBuilder;
        this.pidSerializer = pidSerializer;
        this.credentialIssuerIdentifier = credentialIssuerIdentifier;
    }

    @Override
    public void processTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        var pidCredentialData = pidSerializer.fromString(session.getCheckedParameter(SessionKey.IDENTIFICATION_DATA));
        var dpopPublicKey = session.getCheckedParameterAsJwk(SessionKey.DPOP_PUBLIC_KEY);

        String seedPid = seedPidBuilder.build(pidCredentialData, dpopPublicKey, credentialIssuerIdentifier);

        response.withJsonBody(objectMapper.createObjectNode().put(REFRESH_TOKEN, seedPid));
    }
}
