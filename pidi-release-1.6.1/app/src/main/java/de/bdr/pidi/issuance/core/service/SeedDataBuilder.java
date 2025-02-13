/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.core.service;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.SeedPidBuilder;

import java.time.Instant;

public class SeedDataBuilder {
    private PidCredentialData pidCredentialData;
    private JWK holderBindingKey = null;
    private JWK clientInstanceKey = null;
    private JWK pinDerivedKey = null;
    private String issuerId;
    private Instant issuedAt;
    private Instant expiresAt;

    public SeedDataBuilder setPidCredentialData(PidCredentialData pidCredentialData) {
        this.pidCredentialData = pidCredentialData;
        return this;
    }

    public SeedDataBuilder setHolderBindingKey(JWK holderBindingKey) {
        this.holderBindingKey = holderBindingKey;
        return this;
    }

    public SeedDataBuilder setClientInstanceKey(JWK clientInstanceKey) {
        this.clientInstanceKey = clientInstanceKey;
        return this;
    }

    public SeedDataBuilder setPinDerivedKey(JWK pinDerivedKey) {
        this.pinDerivedKey = pinDerivedKey;
        return this;
    }

    public SeedDataBuilder setIssuerId(String issuerId) {
        this.issuerId = issuerId;
        return this;
    }

    public SeedDataBuilder setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public SeedDataBuilder setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public SeedPidBuilder.SeedData build() {
        return new SeedPidBuilder.SeedData(pidCredentialData, holderBindingKey, clientInstanceKey, pinDerivedKey, issuerId, issuedAt, expiresAt);
    }
}