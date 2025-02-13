/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.exception;

public class ParameterTooLongException extends OIDException {
    public ParameterTooLongException(String parameter, int maxLength) {
        super("invalid_request", "The " + parameter + " parameter exceeds the maximum permitted size of " + maxLength + " bytes");
    }
}
