/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.exception;

import de.bdr.revocation.identification.core.AuthenticationException;
import de.bdr.revocation.identification.core.IdentificationException;

public class AuthenticationNotFoundException extends IdentificationException
    implements AuthenticationException {

    public static final int AUTHENTICATION_NOT_FOUND = IdentificationException.BASE_ERROR_CODE + 9;

    private final String securityMessage;

    public AuthenticationNotFoundException(String securityMessage) {
        super(AuthenticationException.NO_VALID_AUTH, AUTHENTICATION_NOT_FOUND);
        this.securityMessage = securityMessage;
    }

    @Override
    public String getSecurityMessage() {
        return securityMessage;
    }
}
