/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
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
