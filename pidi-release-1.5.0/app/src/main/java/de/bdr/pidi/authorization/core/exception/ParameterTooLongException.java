/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.exception;

public class ParameterTooLongException extends OIDException {
    public ParameterTooLongException(String parameter, int maxLength) {
        super("invalid_request", "The " + parameter + " parameter exceeds the maximum permitted size of " + maxLength + " bytes");
    }
}
