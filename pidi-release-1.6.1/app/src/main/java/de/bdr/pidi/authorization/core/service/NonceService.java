/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.service;

import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.Nonce;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface NonceService {
    /**
     * generate a nonce intended for use in a DPoP
     * and associates it with the session
     */
    Nonce generateAndStoreDpopNonce(WSession session);

    /**
     * fetch the nonce from the session.
     * @param session
     * @throws de.bdr.pidi.authorization.core.exception.SessionNotFoundException when no nonce is found in the session
     * @throws de.bdr.pidi.base.PidServerException when no nonce expiration time is found in the session
     * @return the previously stored nonce
     */
    Nonce fetchDpopNonceFromSession(WSession session);
}
