/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.exception;


import de.bdr.revocation.identification.core.IdentificationException;

public class SamlResponseException extends IdentificationException
        implements EidWrappingException {

    private static final int SAML_RESPONSE_GENERIC = IdentificationException.BASE_ERROR_CODE + 0x106;

    public SamlResponseException(String message, Throwable cause) {
        super(message, SAML_RESPONSE_GENERIC, cause);
    }

    @Override
    public String getVisibleCode() {
        return "ERR_RESP";
    }

    @Override
    public boolean inSamlRequest() {
        return false;
    }
}
