/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.core.service;

import javax.crypto.SecretKey;

public record SeedEncrypter(String keyIdentifier, SecretKey key) {
}
