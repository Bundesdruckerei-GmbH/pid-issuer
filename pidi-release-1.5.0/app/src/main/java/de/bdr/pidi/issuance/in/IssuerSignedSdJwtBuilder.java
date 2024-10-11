/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.in;

import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest;
import de.bdr.pidi.issuance.core.service.PidSdJwtVcCreator;

public class IssuerSignedSdJwtBuilder extends SdJwtBuilderImpl<SdJwtVcCredentialRequest> {
    private final PidSdJwtVcCreator issuerSigSdJwtVcCreator;

    public IssuerSignedSdJwtBuilder(PidSdJwtVcCreator issuerSigSdJwtVcCreator) {
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
