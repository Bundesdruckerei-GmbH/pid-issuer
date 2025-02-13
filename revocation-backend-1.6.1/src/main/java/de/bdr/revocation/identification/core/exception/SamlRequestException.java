/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.exception;


import de.bdr.revocation.identification.core.IdentificationException;

public class SamlRequestException extends IdentificationException
        implements EidWrappingException {

    private static final int SAML_REQUEST_GENERIC = IdentificationException.BASE_ERROR_CODE + 0x105;

    public SamlRequestException(String message, Throwable cause) {
        super(message, SAML_REQUEST_GENERIC, cause);
    }

    @Override
    public String getVisibleCode() {
        return "ERR_REQ";
    }

    @Override
    public boolean inSamlRequest() {
        return true;
    }
}
