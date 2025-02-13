/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.out.issuance;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;

import java.time.Instant;

public interface SeedPidBuilder {

    /**
     * Encodes the PID credential data and the device public key into a Seed PID.
     *
     * @param pidCredentialData
     * @param holderBindingKey
     * @param issuerId
     * @return a string representation of the Seed PID (JWT in compact serialization format)
     * @throws SeedException to signal errors
     */
    String build(PidCredentialData pidCredentialData, JWK holderBindingKey, String issuerId)
            throws SeedException;

    /**
     * Puts the PID credential data as json, the client instance key and the pin derived key into a Seed PID.
     *
     * @param pidCredentialData
     * @param clientInstanceKey
     * @param pinDerivedPublicKey
     * @param issuerId
     * @return a string representation of the Seed PID (JWT in compact serialization format)
     * @throws SeedException to signal errors
     */
    String build(PidCredentialData pidCredentialData, JWK clientInstanceKey, JWK pinDerivedPublicKey, String issuerId)
            throws SeedException;

    /**
     * Extracts the data from the Seed PID and verifies its integrity
     *
     * @param seedPid  a string representation (JWT in compact serialization format)
     * @param issuerId
     * @return the seed's data
     * @throws SeedException
     */
    EncSeedData extractVerifiedEncSeed(String seedPid, String issuerId)
            throws SeedException;

    /**
     * Extracts the data from the Seed PID and verifies its integrity
     *
     * @param seedPid  a string representation (JWT in compact serialization format)
     * @param issuerId
     * @return the seed's data
     * @throws SeedException
     */
    PinSeedData extractVerifiedPinSeed(String seedPid, String issuerId)
            throws SeedException;

    record SeedData(
            PidCredentialData pidCredentialData,
            JWK holderBindingKey,
            JWK clientInstanceKey,
            JWK pinDerivedKey,
            String issuerId,
            Instant issuedAt,
            Instant expiresAt) implements PinSeedData, EncSeedData {

        public SeedData(PidCredentialData pidCredentialData, JWK holderBindingKey, String issuerId, Instant issuedAt, Instant expiresAt) {
            this(pidCredentialData, holderBindingKey, null, null, issuerId, issuedAt, expiresAt);
        }

        public SeedData(PidCredentialData pidCredentialData, JWK clientInstanceKey, JWK pinDerivedKey, String issuerId, Instant issuedAt, Instant expiresAt) {
            this(pidCredentialData, null, clientInstanceKey, pinDerivedKey, issuerId, issuedAt, expiresAt);
        }
    }

    interface PinSeedData {
        PidCredentialData pidCredentialData();
        JWK clientInstanceKey();
        JWK pinDerivedKey();
        String issuerId();
        Instant issuedAt();
        Instant expiresAt();
    }

    interface EncSeedData {
        PidCredentialData pidCredentialData();
        JWK holderBindingKey();
        String issuerId();
        Instant issuedAt();
        Instant expiresAt();
    }
}
