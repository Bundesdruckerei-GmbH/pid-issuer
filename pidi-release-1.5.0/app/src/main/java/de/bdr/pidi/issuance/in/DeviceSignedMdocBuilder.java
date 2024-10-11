/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.in;

import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.MdocBuilder;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialRequest;
import de.bdr.pidi.issuance.core.service.DeviceSignedMdocCreator;

import java.security.interfaces.ECPrivateKey;
import java.util.UUID;

public class DeviceSignedMdocBuilder implements MdocBuilder<MsoMdocAuthChannelCredentialRequest> {
    private final DeviceSignedMdocCreator mdocCreator;
    private final ECPrivateKey signerPrivateKey;

    public DeviceSignedMdocBuilder(DeviceSignedMdocCreator mdocCreator, ECPrivateKey signerPrivateKey) {
        this.mdocCreator = mdocCreator;
        this.signerPrivateKey = signerPrivateKey;
    }

    @Override
    public String build(PidCredentialData pidCredentialData, MsoMdocAuthChannelCredentialRequest credentialRequest, String holderBindingKey) {
        UUID dataKey = UUID.randomUUID();
        try {
            mdocCreator.putPidCredentialData(dataKey, pidCredentialData);
            return mdocCreator.create(credentialRequest, dataKey, signerPrivateKey);
        } finally {
            mdocCreator.removePidCredentialData(dataKey);
        }
    }
}
