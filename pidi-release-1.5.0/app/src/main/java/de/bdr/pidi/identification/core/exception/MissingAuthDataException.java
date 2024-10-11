/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core.exception;

import lombok.Getter;

@Getter
public class MissingAuthDataException extends RuntimeException {

    private final String securityMessage;

    public MissingAuthDataException(String message) {
        super("No valid auth");
        this.securityMessage = message;
    }

}
