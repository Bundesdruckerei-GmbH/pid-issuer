/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core;

import de.bdr.pidi.authorization.core.domain.Nonce;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Duration;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NonceFactory {

    /**
     * Generates a nonce as a secure random string consisting of 22 characters of the set [a-zA-Z0-9]
     */
    public static Nonce createSecureRandomNonce(Duration expiresIn) {
        return new Nonce(RandomUtil.randomString(), expiresIn);
    }
}
