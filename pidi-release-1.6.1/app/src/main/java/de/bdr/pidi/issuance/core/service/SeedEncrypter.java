/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */

package de.bdr.pidi.issuance.core.service;

import javax.crypto.SecretKey;

public record SeedEncrypter(String keyIdentifier, SecretKey key) {
}
