/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
