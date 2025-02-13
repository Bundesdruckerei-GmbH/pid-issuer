/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.service;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.exception.InvalidGrantException;
import de.bdr.pidi.authorization.core.exception.OIDException;
import de.bdr.pidi.authorization.core.util.PinUtil;
import de.bdr.pidi.authorization.out.persistence.PinRetryCounterAdapter;
import de.bdr.pidi.base.PidServerException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class PinRetryCounterService {
    private final PinRetryCounterAdapter adapter;
    private final int maxPinRetries;
    private final Duration validity;

    public PinRetryCounterService(PinRetryCounterAdapter adapter, AuthorizationConfiguration authConfig) {
        this.adapter = adapter;
        this.maxPinRetries = authConfig.getMaxPinRetries();
        this.validity = authConfig.getPinRetryCounterValidity();
    }

    public void initPinRetryCounter(JWK clientInstanceKey) {
        var pinRetryCounterId = PinUtil.computeRetryCounterId(clientInstanceKey);
        adapter.create(pinRetryCounterId, validity);
    }

    public String loadPinCounter(JWK clientInstanceKey) {
        var pinRetryCounterId = PinUtil.computeRetryCounterId(clientInstanceKey);
        var pinRetryCounter = adapter.find(pinRetryCounterId)
                // seed credential has been validated, so a problem would be on our side
                .orElseThrow(() -> new PidServerException("Pin retry counter not found"));
        if (pinRetryCounter.getValue() >= maxPinRetries) {
            throw new InvalidGrantException("PIN locked");
        }
        return pinRetryCounterId;
    }

    public void increment(String pinRetryCounterId, OIDException e) {
        var retryCounter = adapter.find(pinRetryCounterId).orElseThrow(() -> new PidServerException("Pin retry counter not found"));
        retryCounter.increment();
        adapter.increment(retryCounter);
        if (retryCounter.getValue() >= maxPinRetries) {
            throw new InvalidGrantException("PIN locked", e);
        }
    }
}
