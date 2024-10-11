/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.core.service;

import com.nimbusds.jose.JWSSigner;

public record SeedSigner(String keyIdentifier, JWSSigner signer) {
}
