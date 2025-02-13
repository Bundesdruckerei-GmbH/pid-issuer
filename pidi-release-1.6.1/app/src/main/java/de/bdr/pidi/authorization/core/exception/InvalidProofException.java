/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.exception;

public class InvalidProofException extends OIDException {
    public InvalidProofException(String message) {
        super("invalid_proof", message);
    }

    public InvalidProofException(String message, Throwable cause) {
        super("invalid_proof", message, cause);
    }
}
