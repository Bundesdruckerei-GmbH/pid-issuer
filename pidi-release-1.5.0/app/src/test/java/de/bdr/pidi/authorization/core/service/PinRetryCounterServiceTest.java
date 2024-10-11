/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.service;

import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.domain.PinRetryCounter;
import de.bdr.pidi.authorization.core.exception.InvalidGrantException;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.util.PinUtil;
import de.bdr.pidi.authorization.out.persistence.PinRetryCounterAdapter;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static de.bdr.pidi.authorization.ConfigTestData.AUTH_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PinRetryCounterServiceTest {

    private static final String RETRY_COUNTER_ID = PinUtil.computeRetryCounterId(TestUtils.DEVICE_PUBLIC_KEY);

    @Mock
    private PinRetryCounterAdapter adapter;
    @Spy
    private AuthorizationConfiguration authConfig = AUTH_CONFIG;
    @InjectMocks
    private PinRetryCounterService service;

    @DisplayName("Validate init pin retry counter")
    @Test
    void test001() {
        service.initPinRetryCounter(TestUtils.DEVICE_PUBLIC_KEY);
        verify(adapter).create(RETRY_COUNTER_ID, AUTH_CONFIG.getPinRetryCounterValidity());
    }

    @DisplayName("Validate load pin retry counter")
    @Test
    void test002() {
        doReturn(Optional.of(getPinRetryCounter(0))).when(adapter).find(RETRY_COUNTER_ID);

        var counter = service.loadPinCounter(TestUtils.DEVICE_PUBLIC_KEY);

        assertThat(counter).isEqualTo(RETRY_COUNTER_ID);
    }

    @DisplayName("Validate exception when try to load exceeded pin retry counter")
    @Test
    void test003() {
        doReturn(Optional.of(getPinRetryCounter(AUTH_CONFIG.getMaxPinRetries()))).when(adapter).find(RETRY_COUNTER_ID);

        assertThatThrownBy(() -> service.loadPinCounter(TestUtils.DEVICE_PUBLIC_KEY))
                .isInstanceOf(InvalidGrantException.class)
                .hasMessage("PIN locked");
    }

    @DisplayName("Validate incremented pin retry count")
    @Test
    void test004() {
        doReturn(Optional.of(getPinRetryCounter(0))).when(adapter).find(RETRY_COUNTER_ID);

        service.increment(RETRY_COUNTER_ID, new InvalidRequestException("something was wrong"));

        verify(adapter).increment(argThat(p -> p.getValue() == 1));
    }

    @DisplayName("Validate PIN locked when increment exceeded pin retries")
    @Test
    void test005() {
        doReturn(Optional.of(getPinRetryCounter(AUTH_CONFIG.getMaxPinRetries() - 1))).when(adapter).find(RETRY_COUNTER_ID);

        var cause = new InvalidRequestException("something was wrong");
        assertThatThrownBy(() -> service.increment(RETRY_COUNTER_ID, cause))
                .isInstanceOf(InvalidGrantException.class)
                .hasMessage("PIN locked")
                .hasCauseInstanceOf(InvalidRequestException.class);

        verify(adapter).increment(argThat(p -> p.getValue() == AUTH_CONFIG.getMaxPinRetries()));
    }

    private PinRetryCounter getPinRetryCounter(int count) {
        return new PinRetryCounter(1L, RETRY_COUNTER_ID, count, Instant.now().plusSeconds(30));
    }
}