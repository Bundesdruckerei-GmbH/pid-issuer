/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.exception;


import de.bdr.revocation.identification.core.AuthenticationException;
import de.bdr.revocation.identification.core.IdentificationException;

public class AuthenticationStateException extends IdentificationException
    implements AuthenticationException {

    public static final int AUTHENTICATION_STATE = IdentificationException.BASE_ERROR_CODE + 10;

    private final String securityMessage;

    public AuthenticationStateException(String securityMessage) {
        this(AuthenticationException.NO_VALID_AUTH, securityMessage);
    }

    public AuthenticationStateException(String message, String securityMessage) {
        super(message, AUTHENTICATION_STATE);
        this.securityMessage = securityMessage;
    }

    @Override
    public String getSecurityMessage() {
        return securityMessage;
    }
}
