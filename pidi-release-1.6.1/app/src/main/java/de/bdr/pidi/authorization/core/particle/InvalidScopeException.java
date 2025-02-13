/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.pidi.authorization.core.exception.OIDException;

public class InvalidScopeException extends OIDException {

    public InvalidScopeException(String message) {
        super("invalid_scope", message);
    }
}
