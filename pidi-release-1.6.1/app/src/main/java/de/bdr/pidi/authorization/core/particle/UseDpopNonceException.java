/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.pidi.authorization.core.exception.OIDException;

import static de.bdr.pidi.authorization.core.particle.DpopHandler.DPOP_NONCE_HEADER;

public class UseDpopNonceException extends OIDException {

    public UseDpopNonceException(String nonce, String message) {
        super("use_dpop_nonce", message);
        super.addHeader(DPOP_NONCE_HEADER, nonce);
    }
}
