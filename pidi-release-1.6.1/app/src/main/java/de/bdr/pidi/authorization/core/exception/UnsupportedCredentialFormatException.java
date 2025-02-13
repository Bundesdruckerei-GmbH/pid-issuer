/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
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
