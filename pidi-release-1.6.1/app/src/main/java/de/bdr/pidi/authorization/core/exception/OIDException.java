/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for exceptions during the OpenID protocol flow
 */
public abstract class OIDException extends RuntimeException {

    private final String logMessage;
    private final String errorCode;
    private final Map<String, String> header = new HashMap<>();

    protected OIDException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        logMessage = message;
    }

    protected OIDException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        logMessage = message;
    }

    protected OIDException(String errorCode, String message, String logMessage) {
        super(message);
        this.errorCode = errorCode;
        this.logMessage = logMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /** the logged message (internally). */
    public String getLogMessage() {
        return logMessage;
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public void addHeader(String key, String value) {
        header.put(key, value);
    }
}
