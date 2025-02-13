/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.in;

import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.issuance.core.service.PidSdJwtVcCreator;
import de.bdr.pidi.issuance.out.revoc.RevocationAdapter;
import de.bdr.pidi.issuance.out.sls.StatusListAdapter;

import java.time.Duration;

public class IssuerSignedSdJwtBuilder extends SdJwtBuilderImpl<SdJwtVcCredentialRequest> {
    private final PidSdJwtVcCreator issuerSigSdJwtVcCreator;

    public IssuerSignedSdJwtBuilder(PidSdJwtVcCreator issuerSigSdJwtVcCreator, Duration lifetime,
                                    StatusListAdapter statusListAdapter, RevocationAdapter revocationAdapter,
                                    FlowVariant flowVariant) {
        super(statusListAdapter, revocationAdapter, lifetime, flowVariant);
        this.issuerSigSdJwtVcCreator = issuerSigSdJwtVcCreator;
    }

    @Override
    PidSdJwtVcCreator provideCreator(SdJwtVcCredentialRequest credentialRequest) {
        return issuerSigSdJwtVcCreator;
    }

    @Override
    String getVct(SdJwtVcCredentialRequest credentialRequest) {
        return credentialRequest.getVct();
    }
}
