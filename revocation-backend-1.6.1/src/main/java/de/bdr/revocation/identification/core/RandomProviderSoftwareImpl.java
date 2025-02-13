/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;

@Component
public class RandomProviderSoftwareImpl {

    private final Logger log = LoggerFactory.getLogger(RandomProviderSoftwareImpl.class);

    private Random tokenRng;
    private Random sessionRng;
    private Random samlRng;


    private synchronized Random sessionRng() {
        if (sessionRng == null) {
            sessionRng = initRng();
        }
        return sessionRng;
    }

    private Random initRng() {
        log.debug("~~~~ consuming entropy for RNG");
        SecureRandom instanceStrong = new SecureRandom();
        int rand = instanceStrong.nextInt();
        if (log.isTraceEnabled()) {
            log.trace("~~~~ created RNG, first random int: {}, algo: {}, provider: {}", rand, instanceStrong.getAlgorithm(), instanceStrong.getProvider());
        }
        return instanceStrong;
    }


    private synchronized Random tokenRng() {
        if (tokenRng == null) {
            tokenRng = initRng();
        }
        return tokenRng;
    }

    private synchronized Random samlRng() {
        if (samlRng == null) {
            samlRng = initRng();
        }
        return samlRng;
    }

    public Random getTokenRng() {
        return tokenRng();
    }

    public Random getSessionRng() {
        return sessionRng();
    }

    public Random getSamlRng() {
        return samlRng();
    }
}
