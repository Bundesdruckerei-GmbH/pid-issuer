/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
