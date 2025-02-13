/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.service;

import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.NonceFactory;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.Nonce;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.SessionNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class NonceServiceImpl implements NonceService {
    private final Duration dpopNonceLifetime;

    public NonceServiceImpl(AuthorizationConfiguration configuration) {
        this.dpopNonceLifetime = configuration.getDpopNonceLifetime();
    }

    @Override
    public Nonce generateAndStoreDpopNonce(WSession session) {
        Nonce result = NonceFactory.createSecureRandomNonce(dpopNonceLifetime);
        session.putParameter(SessionKey.DPOP_NONCE, result.nonce());
        session.putParameter(SessionKey.DPOP_NONCE_EXP_TIME, result.expirationTime());
        return result;
    }

    @Override
    public Nonce fetchDpopNonceFromSession(WSession session) {
        var value = session.getOptionalParameter(SessionKey.DPOP_NONCE).orElseThrow(SessionNotFoundException::new);
        var expires = session.getCheckedParameterAsInstant(SessionKey.DPOP_NONCE_EXP_TIME);
        return new Nonce(value, this.dpopNonceLifetime, expires);
    }
}
