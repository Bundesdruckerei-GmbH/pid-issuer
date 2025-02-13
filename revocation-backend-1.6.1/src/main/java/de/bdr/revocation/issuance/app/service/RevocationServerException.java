/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.app.service;

public class RevocationServerException extends RuntimeException {
    public RevocationServerException(String message, Exception e) {
        super(message, e);
    }

    public RevocationServerException(String message) {
        super(message);
    }
}
