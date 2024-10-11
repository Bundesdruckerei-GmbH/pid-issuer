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
import de.bdr.pidi.base.requests.SeedCredentialRequest;

public class SeedCredentialIssuanceHandler implements OidHandler {
    // Response keys
    public static final String CREDENTIAL = "credential";

    private final SeedPidBuilder seedPidBuilder;
    private final PidSerializer pidSerializer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String credentialIssuerIdentifier;

    public SeedCredentialIssuanceHandler(SeedPidBuilder seedPidBuilder, PidSerializer pidSerializer, String credentialIssuerIdentifier) {
        this.seedPidBuilder = seedPidBuilder;
        this.pidSerializer = pidSerializer;
        this.credentialIssuerIdentifier = credentialIssuerIdentifier;
    }

    @Override
    public void processSeedCredentialRequest(HttpRequest<SeedCredentialRequest> request, WResponseBuilder response, WSession session) {
        var pidCredentialData = pidSerializer.fromString(session.getCheckedParameter(SessionKey.IDENTIFICATION_DATA));
        var dpopJwk = session.getCheckedParameterAsJwk(SessionKey.DPOP_PUBLIC_KEY);
        var pinDerivedPublicKey = session.getCheckedParameterAsJwk(SessionKey.PIN_DERIVED_PUBLIC_KEY);

        String seedPid = seedPidBuilder.build(pidCredentialData, dpopJwk, pinDerivedPublicKey, credentialIssuerIdentifier);

        response.withJsonBody(objectMapper.createObjectNode().put(CREDENTIAL, seedPid));
    }
}
