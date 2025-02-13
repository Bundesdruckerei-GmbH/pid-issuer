/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
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
