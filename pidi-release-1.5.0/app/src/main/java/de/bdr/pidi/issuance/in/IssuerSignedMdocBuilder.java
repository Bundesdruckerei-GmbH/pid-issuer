/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.in;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialRequest;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.MdocBuilder;
import de.bdr.pidi.issuance.core.service.IssuerSignedMdocCreator;

import java.text.ParseException;
import java.util.UUID;

public class IssuerSignedMdocBuilder implements MdocBuilder<MsoMdocCredentialRequest> {
    private final IssuerSignedMdocCreator mdocCreator;

    public IssuerSignedMdocBuilder(IssuerSignedMdocCreator mdocCreator) {
        this.mdocCreator = mdocCreator;
    }

    @Override
    public String build(PidCredentialData pidCredentialData, MsoMdocCredentialRequest credentialRequest, String holderBindingKey) throws ParseException {
        UUID dataKey = UUID.randomUUID();
        try {
            mdocCreator.putPidCredentialData(dataKey, pidCredentialData);
            return mdocCreator.create(credentialRequest, dataKey, JWK.parse(holderBindingKey), null);
        } finally {
            mdocCreator.removePidCredentialData(dataKey);
        }
    }
}
