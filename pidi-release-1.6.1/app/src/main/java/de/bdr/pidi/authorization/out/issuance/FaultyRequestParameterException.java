/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.out.issuance;

public class FaultyRequestParameterException extends RuntimeException {

    public FaultyRequestParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public FaultyRequestParameterException(String message) {
        super(message);
    }
}
