/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
