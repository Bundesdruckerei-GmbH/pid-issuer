/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.exception;

public class UnsupportedCredentialFormatException extends OIDException {
    public UnsupportedCredentialFormatException(String message) {
        super("unsupported_credential_format", message);
    }

    public UnsupportedCredentialFormatException(String message, Throwable cause) {
        super("unsupported_credential_format", message, cause);
    }
}
