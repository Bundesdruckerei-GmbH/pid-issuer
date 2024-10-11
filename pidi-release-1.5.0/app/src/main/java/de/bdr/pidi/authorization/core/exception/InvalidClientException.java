/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.exception;

public class InvalidClientException extends OIDException {
    public InvalidClientException(String message) {
        super("invalid_client", message);
    }
    public InvalidClientException(String message, Throwable cause) {
        super("invalid_client", message, cause);
    }
}
