/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
