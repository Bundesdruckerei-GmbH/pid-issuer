/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.exception;

public class UnsupportedGrantTypeException extends OIDException {
    public UnsupportedGrantTypeException(String message) {
        super("unsupported_grant_type", message);
    }
}
