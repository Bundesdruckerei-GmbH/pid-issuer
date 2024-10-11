/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.exception;

public class UnsupportedGrantTypeException extends OIDException {
    public UnsupportedGrantTypeException(String message) {
        super("unsupported_grant_type", message);
    }
}
