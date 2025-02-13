/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.core.service;

import com.nimbusds.jose.JWSSigner;

public record SeedSigner(String keyIdentifier, JWSSigner signer) {
}
