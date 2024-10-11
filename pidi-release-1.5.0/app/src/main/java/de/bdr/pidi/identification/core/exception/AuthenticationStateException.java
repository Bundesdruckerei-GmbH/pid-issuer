/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
