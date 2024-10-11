/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.exception;

import lombok.Getter;

@Getter
public class FinishAuthException extends RuntimeException {
    private final String redirectUri;
    private final String state;

    public FinishAuthException(String redirectUri, String state, RuntimeException cause) {
        super(cause);
        this.redirectUri = redirectUri;
        this.state = state;
    }
}
