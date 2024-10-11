/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.walletattestation;

public record WalletAttestationRequest(ClientAttestationJwt clientAttestationJwt, ClientAttestationPopJwt clientAttestationPopJwt, String clientId, String issuerIdentifier) {

}
