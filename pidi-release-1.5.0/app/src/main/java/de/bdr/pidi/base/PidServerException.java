/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.base;

public class PidServerException extends RuntimeException {
    public PidServerException(String message) {
        super(message);
    }

    public PidServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
