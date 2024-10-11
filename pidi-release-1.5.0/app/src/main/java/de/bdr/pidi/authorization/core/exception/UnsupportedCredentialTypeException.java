/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.exception;

public class UnsupportedCredentialTypeException extends OIDException {
    public UnsupportedCredentialTypeException(String message) {
        super("unsupported_credential_type", message);
    }
}
