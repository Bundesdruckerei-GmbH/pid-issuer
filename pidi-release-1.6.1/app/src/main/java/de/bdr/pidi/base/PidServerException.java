/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
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
