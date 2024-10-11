/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core.exception;

public class SamlCryptoConfigException extends RuntimeException
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
