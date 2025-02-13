/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.exception;

public class InvalidCredentialRequestException extends OIDException {
    public InvalidCredentialRequestException(String message, Throwable cause) {
        super("invalid_credential_request", message, cause);
    }

    public InvalidCredentialRequestException(String message) {
        super("invalid_credential_request", message);
    }
}
