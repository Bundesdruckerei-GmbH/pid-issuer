/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.out.persistence;

import de.bdr.pidi.authorization.core.domain.PinRetryCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PinRetryCounterAdapter {
    private final PinRetryCounterRepository pinRetryCounterRepository;

    /**
     * creates a counter for new digests, overrides a counter with initial values on already present digests
     */
    @Transactional
    public PinRetryCounter create(final String digest, final Duration validity) {
        var entity = pinRetryCounterRepository.findFirstByDigest(digest).orElseGet(PinRetryCounterEntity::new);
        entity.setDigest(digest);
        // Postgress timestamp contains only microseconds and no nanoseconds
        entity.setExpires(Instant.now().plus(validity).truncatedTo(ChronoUnit.MICROS));
        entity.setValue(0);
        pinRetryCounterRepository.save(entity);
        return map(entity);
    }

    public Optional<PinRetryCounter> find(final String digest) {
        return pinRetryCounterRepository.findFirstByDigest(digest).map(PinRetryCounterAdapter::map);
    }

    @Transactional
    public void increment(final PinRetryCounter pinRetryCounter) {
        PinRetryCounterEntity entity = pinRetryCounterRepository.getReferenceById(pinRetryCounter.getId());
        if (entity.getValue() + 1 != pinRetryCounter.getValue()) {
            throw new IllegalArgumentException("PinRetryCounter is invalid.");
        }
        entity.setValue(pinRetryCounter.getValue());
        pinRetryCounterRepository.save(entity);
    }

    private static PinRetryCounter map(final PinRetryCounterEntity entity) {
        return new PinRetryCounter(entity.getId(), entity.getDigest(), entity.getValue(), entity.getExpires());
    }
}
