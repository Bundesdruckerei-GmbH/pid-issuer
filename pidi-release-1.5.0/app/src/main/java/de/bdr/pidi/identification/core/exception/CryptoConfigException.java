/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
