/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.exception;

public class ValidationFailedException extends InvalidRequestException {
    public ValidationFailedException(String message) {
        super(message);
    }
    public ValidationFailedException(String message, String logMessage) {
        super(message, logMessage);
    }
}
