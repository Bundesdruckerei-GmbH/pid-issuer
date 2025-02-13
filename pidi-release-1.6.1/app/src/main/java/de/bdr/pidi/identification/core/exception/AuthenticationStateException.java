/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core.exception;

import lombok.Getter;

@Getter
public class AuthenticationStateException extends RuntimeException {
    private final String securityMessage;

    public AuthenticationStateException(String securityMessage) {
        this("No valid authentication", securityMessage);
    }

    public AuthenticationStateException(String message, String securityMessage) {
        super(message);
        this.securityMessage = securityMessage;
    }
}
