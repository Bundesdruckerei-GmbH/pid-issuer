/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.out.random;

import de.bdr.pidi.identification.core.RandomProvider;
import lombok.extern.slf4j.Slf4j;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;

@Slf4j
@Component
@SecondaryAdapter
public class RandomProviderSoftwareImpl implements RandomProvider {

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

    @Override
    public Random getTokenRng() {
        return tokenRng();
    }

    @Override
    public Random getSessionRng() {
        return sessionRng();
    }

    @Override
    public Random getSamlRng() {
        return samlRng();
    }
}
