/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.in;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.openid4vc.common.vci.FormatSpecificCredentialRequest;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.SdJwtBuilder;
import de.bdr.pidi.issuance.core.service.PidSdJwtVcCreator;

import java.text.ParseException;
import java.util.UUID;

abstract class SdJwtBuilderImpl<T extends FormatSpecificCredentialRequest> implements SdJwtBuilder<T> {

    abstract PidSdJwtVcCreator provideCreator(T credentialRequest);

    abstract String getVct(T credentialRequest);

    @Override
    public String build(PidCredentialData pidCredentialData, T credentialRequest, String holderBindingKey) throws ParseException {
        var creator = provideCreator(credentialRequest);
        validateRequest(creator, credentialRequest);
        UUID dataKey = UUID.randomUUID();
        try {
            creator.putPidCredentialData(dataKey, pidCredentialData);
            return creator.create(credentialRequest, dataKey, JWK.parse(holderBindingKey), null);
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
