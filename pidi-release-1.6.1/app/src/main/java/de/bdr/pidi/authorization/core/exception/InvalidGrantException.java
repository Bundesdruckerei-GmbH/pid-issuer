/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.exception;

public class InvalidGrantException extends OIDException {
    public InvalidGrantException(String message) {
        super("invalid_grant", message);
    }
    public InvalidGrantException(String message, Throwable cause) {
        super("invalid_grant", message, cause);
    }
}
