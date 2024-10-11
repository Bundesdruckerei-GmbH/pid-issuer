/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.base;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IssuedAtValidator {

    public static IssuedAtValidationResult validate(Instant issuedAt, Instant timeToCheck, Duration proofTimeTolerance, Duration proofValidity) {
        if (issuedAt == null) {
            return IssuedAtValidationResult.NOT_PRESENT;
        }
        if (issuedAt.minus(proofTimeTolerance).isAfter(timeToCheck)) {
            return IssuedAtValidationResult.IN_FUTURE;
        }
        if (issuedAt.plus(proofTimeTolerance).plus(proofValidity).isBefore(timeToCheck)) {
            return IssuedAtValidationResult.TOO_OLD;
        }
        return IssuedAtValidationResult.VALID;
    }
}

