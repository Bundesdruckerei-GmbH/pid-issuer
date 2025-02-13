/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core.exception;

public class CryptoConfigException
        extends RuntimeException {


    public CryptoConfigException(String message) {
        super(message);
    }

    public CryptoConfigException(String message, Throwable cause) {
        super(message, cause);
    }

}
