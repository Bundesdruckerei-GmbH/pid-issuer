/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.pidi.authorization.core.exception.OIDException;

public class InvalidDpopProofException extends OIDException {

    protected InvalidDpopProofException(String message) {
        super("invalid_dpop_proof", message);
    }
}
