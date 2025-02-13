/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.out.persistence;

import de.bdr.pidi.authorization.core.domain.PinRetryCounter;
import de.bdr.pidi.end2end.integration.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;

import static de.bdr.pidi.authorization.ConfigTestData.AUTH_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PinRetryCounterAdapterTest extends IntegrationTest {

    @Autowired
    private PinRetryCounterAdapter pinRetryCounterAdapter;

    @Autowired
    private PinRetryCounterRepository pinRetryCounterRepository;

    @Test
    void testAdapter() {
        PinRetryCounter pinRetryCounter = pinRetryCounterAdapter.create("digest", AUTH_CONFIG.getPinRetryCounterValidity());

        assertThat(pinRetryCounter).isNotNull();
        assertThat(pinRetryCounter.getValue()).isZero();
        assertThat(pinRetryCounter.getExpirationTime()).isAfter(Instant.now());

        Optional<PinRetryCounter> optionalPinRetryCounter = pinRetryCounterAdapter.find("digest");
        assertThat(optionalPinRetryCounter).isPresent().get().isEqualTo(pinRetryCounter);

        pinRetryCounter.increment();
        assertThatCode(() -> pinRetryCounterAdapter.increment(pinRetryCounter)).doesNotThrowAnyException();
        assertThat(pinRetryCounterRepository.findFirstByDigest("digest")).isPresent().map(PinRetryCounterEntity::getValue).isEqualTo(Optional.of(1));

        pinRetryCounter.increment();
        pinRetryCounter.increment();
        assertThatCode(() -> pinRetryCounterAdapter.increment(pinRetryCounter))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("PinRetryCounter is invalid.");

        pinRetryCounterAdapter.create("digest", AUTH_CONFIG.getPinRetryCounterValidity());
        assertThat(pinRetryCounterRepository.findFirstByDigest("digest")).isPresent().map(PinRetryCounterEntity::getValue).isEqualTo(Optional.of(0));
    }
}