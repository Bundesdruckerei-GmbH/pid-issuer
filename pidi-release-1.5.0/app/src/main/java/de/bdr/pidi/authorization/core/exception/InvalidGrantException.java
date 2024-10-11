/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
