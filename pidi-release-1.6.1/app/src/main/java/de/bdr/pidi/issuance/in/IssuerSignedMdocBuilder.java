/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.in;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.MdocBuilder;
import de.bdr.pidi.issuance.core.service.IssuerSignedMdocCreator;
import de.bdr.pidi.issuance.out.revoc.RevocationAdapter;
import de.bdr.pidi.issuance.out.sls.StatusListAdapter;
import lombok.RequiredArgsConstructor;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
public class IssuerSignedMdocBuilder implements MdocBuilder<MsoMdocCredentialRequest> {
    private final IssuerSignedMdocCreator mdocCreator;
    private final StatusListAdapter statusListAdapter;
    private final RevocationAdapter revocationAdapter;
    private final Duration lifetime;

    @Override
    public String build(PidCredentialData pidCredentialData, MsoMdocCredentialRequest credentialRequest, String holderBindingKey, FlowVariant flowVariant) throws ParseException {
        UUID dataKey = UUID.randomUUID();
        try {
            mdocCreator.putPidCredentialData(dataKey, pidCredentialData);
            var statusRef = statusListAdapter.acquireFreeIndex(flowVariant);
            var mdoc = mdocCreator.create(credentialRequest, dataKey, JWK.parse(holderBindingKey), statusRef).getCredential();
            // The expiry time can differ by a few ms, but otherwise the credential would have to be parsed to get to the exact time.
            var exp = Instant.now().plus(lifetime);
            revocationAdapter.notifyRevocService(pidCredentialData.getPseudonym(), statusRef, exp);
            return mdoc;
        } finally {
            mdocCreator.removePidCredentialData(dataKey);
        }
    }
}
