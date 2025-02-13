/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.exception;

import de.bdr.revocation.identification.core.IdentificationException;

public class SamlCryptoConfigException extends IdentificationException
        implements EidWrappingException {


    private final boolean inRequest;

    public SamlCryptoConfigException(String message, boolean inRequest) {
        super(message);
        this.inRequest = inRequest;
    }

    public SamlCryptoConfigException(String message, boolean inRequest, Throwable cause) {
        super(message, cause);
        this.inRequest = inRequest;
    }

    @Override
    public String getVisibleCode() {
        return EidWrappingException.ERR_CODE_SYSTEM;
    }

    @Override
    public boolean inSamlRequest() {
        return inRequest;
    }
}
