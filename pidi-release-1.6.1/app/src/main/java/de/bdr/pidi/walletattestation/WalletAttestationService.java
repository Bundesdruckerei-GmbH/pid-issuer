/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.walletattestation;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface WalletAttestationService {
    boolean isValidWallet(WalletAttestationRequest request);
}
