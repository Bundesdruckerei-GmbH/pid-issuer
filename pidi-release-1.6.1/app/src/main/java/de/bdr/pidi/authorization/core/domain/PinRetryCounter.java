/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class PinRetryCounter {
    private final long id;
    private final String digest;
    private int value;
    private final Instant expirationTime;

    public void increment() {
        this.value++;
    }
}
