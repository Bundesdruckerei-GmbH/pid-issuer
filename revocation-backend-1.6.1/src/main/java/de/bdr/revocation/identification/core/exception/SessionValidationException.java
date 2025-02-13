/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.exception;

import de.bdr.revocation.identification.core.AuthenticationException;
import de.bdr.revocation.identification.core.IdentificationException;

public class SessionValidationException extends IdentificationException
        implements AuthenticationException {

    public static final int SESSION_VALIDATION = IdentificationException.BASE_ERROR_CODE + 14;

    private final String securityMessage;

    public SessionValidationException(String message, String securityMessage) {
        super(message, SESSION_VALIDATION);
        this.securityMessage = securityMessage;
    }

    @Override
    public String getSecurityMessage() {
        return securityMessage;
    }
}
