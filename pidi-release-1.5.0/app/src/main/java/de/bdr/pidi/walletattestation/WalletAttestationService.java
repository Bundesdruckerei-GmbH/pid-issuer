/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.walletattestation;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface WalletAttestationService {
    boolean isValidWallet(WalletAttestationRequest request);
}
