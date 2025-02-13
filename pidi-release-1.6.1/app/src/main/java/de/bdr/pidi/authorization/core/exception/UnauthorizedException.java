/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.exception;

public class UnauthorizedException extends RuntimeException {
    private final String scheme;
    private final String error;

    public UnauthorizedException(String scheme) {
        this(scheme, null);
    }

    public UnauthorizedException(String scheme, String error) {
        this(scheme, error, (String) null);
    }

    public UnauthorizedException(String scheme, String error, String description) {
        super(description);
        this.scheme = scheme;
        this.error = error;
    }

    public UnauthorizedException(String scheme, String error, RuntimeException cause) {
        super(cause.getMessage(), cause);
        this.scheme = scheme;
        this.error = error;
    }

    public String getScheme() {
        return scheme;
    }

    public String getError() {
        return error;
    }
}
