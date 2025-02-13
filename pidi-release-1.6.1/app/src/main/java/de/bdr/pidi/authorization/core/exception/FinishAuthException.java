/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
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
