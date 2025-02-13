/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.pidi.authorization.core.exception.OIDException;

public class InvalidDpopProofException extends OIDException {

    protected InvalidDpopProofException(String message) {
        super("invalid_dpop_proof", message);
    }
}
