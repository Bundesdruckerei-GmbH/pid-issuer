/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
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
