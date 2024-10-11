/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
