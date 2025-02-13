/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.pidi.authorization.core.exception.OIDException;

public class VerificationFailedException extends OIDException {
    public VerificationFailedException(String message) {
        super("invalid_grant", message);
    }
}
