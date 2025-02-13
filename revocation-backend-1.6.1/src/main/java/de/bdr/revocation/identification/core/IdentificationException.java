/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core;

/**
 * Base Exception to recognize Info Service Exceptions
 */
public class IdentificationException extends RuntimeException {

    public static final int BASE_ERROR_CODE = 0x30000;

    private final int code;

    public IdentificationException(String message) {
        this(message, BASE_ERROR_CODE);
    }

    protected IdentificationException(String message, int code) {
        super(message);
        this.code = code;
    }

    public IdentificationException(String message, Throwable cause) {
        this(message, BASE_ERROR_CODE, cause);
    }

    protected IdentificationException(String message, int code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        if ((code&0xffff0000) != BASE_ERROR_CODE) {
            return BASE_ERROR_CODE;
        }
        return code;
    }
}
