/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.exception;

public class UnsupportedCredentialTypeException extends OIDException {
    public UnsupportedCredentialTypeException(String message) {
        super("unsupported_credential_type", message);
    }
}
