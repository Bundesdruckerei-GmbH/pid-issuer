/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.domain;

import java.time.Duration;
import java.time.Instant;

public record Nonce(String nonce, Duration expiresIn, Instant expirationTime) {
    public Nonce(String nonce, Duration expiresIn) {
        this(nonce, expiresIn, Instant.now().plus(expiresIn));
    }
}
