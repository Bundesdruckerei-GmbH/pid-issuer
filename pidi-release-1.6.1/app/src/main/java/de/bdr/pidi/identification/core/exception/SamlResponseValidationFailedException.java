/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core.exception;

public class SamlResponseValidationFailedException
        extends RuntimeException
        implements EidWrappingException {


    private final String visibleCode;

    public SamlResponseValidationFailedException(String message, String visibleCode, Throwable cause) {
        super(message, cause);
        if (visibleCode == null) {
            visibleCode = EidWrappingException.ERR_CODE_ABORTED;
        }
        this.visibleCode = visibleCode;
    }

    @Override
    public String getVisibleCode() {
        return visibleCode;
    }

    @Override
    public boolean inSamlRequest() {
        return false;
    }
}
