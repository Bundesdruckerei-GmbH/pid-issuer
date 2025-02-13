/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.in;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.openid4vc.common.vci.FormatSpecificCredentialRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.SdJwtBuilder;
import de.bdr.pidi.issuance.core.service.PidSdJwtVcCreator;
import de.bdr.pidi.issuance.out.revoc.RevocationAdapter;
import de.bdr.pidi.issuance.out.sls.StatusListAdapter;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
abstract class SdJwtBuilderImpl<T extends FormatSpecificCredentialRequest> implements SdJwtBuilder<T> {

    abstract PidSdJwtVcCreator provideCreator(T credentialRequest);

    abstract String getVct(T credentialRequest);

    private final StatusListAdapter statusListAdapter;
    private final RevocationAdapter revocationAdapter;
    private final Duration lifetime;
    private final FlowVariant flowVariant;

    @Override
    public String build(PidCredentialData pidCredentialData, T credentialRequest, String holderBindingKey) throws ParseException {
        var creator = provideCreator(credentialRequest);
        validateRequest(creator, credentialRequest);
        UUID dataKey = UUID.randomUUID();
        try {
            creator.putPidCredentialData(dataKey, pidCredentialData);
            var statusRef = statusListAdapter.acquireFreeIndex(flowVariant);
            var sdJwt = creator.create(credentialRequest, dataKey, JWK.parse(holderBindingKey), statusRef).getCredential();
            // The expiry time can differ by a few ms, but otherwise the credential would have to be parsed to get to the exact time.
            var exp = Instant.now().plus(lifetime);
            revocationAdapter.notifyRevocService(pidCredentialData.getPseudonym(), statusRef, exp);
            return sdJwt;
        } finally {
            creator.removePidCredentialData(dataKey);
        }
    }

    /**
     * The request gets already checked in the OidHandler, this only serves as an additional assurance
     */
    private void validateRequest(PidSdJwtVcCreator creator, T request) {
        if (!creator.validateCredentialRequest(request)) {
            throw new IllegalArgumentException("Credential type %s not supported".formatted(getVct(request)));
        }
    }
}
