/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.in;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.MdocBuilder;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialRequest;
import de.bdr.pidi.issuance.core.service.DeviceSignedMdocCreator;
import lombok.RequiredArgsConstructor;

import java.security.interfaces.ECPrivateKey;
import java.util.UUID;

@RequiredArgsConstructor
public class DeviceSignedMdocBuilder implements MdocBuilder<MsoMdocAuthChannelCredentialRequest> {
    private final DeviceSignedMdocCreator mdocCreator;
    private final ECPrivateKey signerPrivateKey;

    @Override
    public String build(PidCredentialData pidCredentialData, MsoMdocAuthChannelCredentialRequest credentialRequest, String holderBindingKey, FlowVariant flowVariant) {
        UUID dataKey = UUID.randomUUID();
        try {
            mdocCreator.putPidCredentialData(dataKey, pidCredentialData);
            return mdocCreator.create(credentialRequest, dataKey, signerPrivateKey);
        } finally {
            mdocCreator.removePidCredentialData(dataKey);
        }
    }
}
