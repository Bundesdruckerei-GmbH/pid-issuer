/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.domain;

import java.time.Duration;
import java.time.Instant;

public record Nonce(String nonce, Duration expiresIn, Instant expirationTime) {
    public Nonce(String nonce, Duration expiresIn) {
        this(nonce, expiresIn, Instant.now().plus(expiresIn));
    }
}
