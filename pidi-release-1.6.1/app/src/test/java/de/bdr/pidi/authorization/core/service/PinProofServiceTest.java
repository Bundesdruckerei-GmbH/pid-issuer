/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.service;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class PinProofServiceTest {
    private static final String AUDIENCE = TestUtils.ISSUER_IDENTIFIER_AUDIENCE + "/" + FlowVariant.B1.urlPath;
    private static final String AUDIENCE_INVALID = "http://test/b1";
    private static final String NONCE = TestUtils.C_NONCE;
    private static final String NONCE_INVALID = "nonce";
    private static final Instant NONCE_EXPIRATION = now();
    private static final Instant NONCE_EXPIRED = NONCE_EXPIRATION.minusSeconds(120);
    private static final Duration PROOF_TOLERANCE = Duration.ofSeconds(30);

    @Mock
    private AuthorizationConfiguration config;

    @Mock
    private WSession session;

    @InjectMocks
    private PinProofServiceImpl service;

    @DisplayName("validate pin_derived_eph_key_pop successful")
    @Test
    void test001() {
        doReturn(PROOF_TOLERANCE).when(config).getProofTimeTolerance();
        doReturn(FlowVariant.B1).when(session).getFlowVariant();
        doReturn(AUDIENCE).when(config).getCredentialIssuerIdentifier(any());
        doReturn(NONCE_EXPIRATION).when(session).getCheckedParameterAsInstant(SessionKey.C_NONCE_EXP_TIME);
        doReturn(NONCE).when(session).getCheckedParameter(SessionKey.C_NONCE);

        var pinDerivedEphKeyPop = TestUtils.PIN_DERIVED_EPH_KEY_POP;
        var pinDerivedEphKey = service.validatePinDerivedEphKeyPop(session, pinDerivedEphKeyPop);

        assertThat(pinDerivedEphKey).isEqualTo(TestUtils.PIN_DERIVED_PUBLIC_KEY);
    }

    @DisplayName("validate pin_derived_eph_key_pop from token request successful")
    @Test
    void test002() {
        doReturn(PROOF_TOLERANCE).when(config).getProofTimeTolerance();
        doReturn(FlowVariant.B1).when(session).getFlowVariant();
        doReturn(AUDIENCE).when(config).getCredentialIssuerIdentifier(any());
        doReturn(NONCE_EXPIRATION).when(session).getCheckedParameterAsInstant(SessionKey.PID_ISSUER_SESSION_ID_EXP_TIME);
        doReturn(NONCE).when(session).getCheckedParameter(SessionKey.PID_ISSUER_SESSION_ID);

        var pinDerivedEphKeyPop = TestUtils.PIN_DERIVED_EPH_KEY_POP;
        var pinDerivedEphKey = service.validatePinDerivedEphKeyPopTokenRequest(session, pinDerivedEphKeyPop);

        assertThat(pinDerivedEphKey).isEqualTo(TestUtils.PIN_DERIVED_PUBLIC_KEY);
    }

    @DisplayName("validate device_key_pop successful")
    @Test
    void test003() {
        doReturn(PROOF_TOLERANCE).when(config).getProofTimeTolerance();
        doReturn(FlowVariant.B1).when(session).getFlowVariant();
        doReturn(AUDIENCE).when(config).getCredentialIssuerIdentifier(any());
        doReturn(NONCE_EXPIRATION).when(session).getCheckedParameterAsInstant(SessionKey.PID_ISSUER_SESSION_ID_EXP_TIME);
        doReturn(NONCE).when(session).getCheckedParameter(SessionKey.PID_ISSUER_SESSION_ID);

        var deviceKeyPop = TestUtils.DEVICE_KEY_POP;
        var deviceKey = service.validateDeviceKeyPopTokenRequest(session, deviceKeyPop);

        assertThat(deviceKey).isEqualTo(TestUtils.DEVICE_PUBLIC_KEY);
    }

    @DisplayName("validate pin_derived_eph_key_pop nonce expired")
    @Test
    void test004() {
        doReturn(NONCE_EXPIRED).when(session).getCheckedParameterAsInstant(SessionKey.C_NONCE_EXP_TIME);
        doReturn(NONCE).when(session).getCheckedParameter(SessionKey.C_NONCE);

        var pinDerivedEphKeyPop = TestUtils.PIN_DERIVED_EPH_KEY_POP;
        assertThatThrownBy(() -> service.validatePinDerivedEphKeyPop(session, pinDerivedEphKeyPop))
                .isInstanceOf(InvalidRequestException.class);
    }

    @DisplayName("validate pin_derived_eph_key_pop from token request session_id expired")
    @Test
    void test005() {
        doReturn(NONCE_EXPIRED).when(session).getCheckedParameterAsInstant(SessionKey.PID_ISSUER_SESSION_ID_EXP_TIME);
        doReturn(NONCE).when(session).getCheckedParameter(SessionKey.PID_ISSUER_SESSION_ID);

        var pinDerivedEphKeyPop = TestUtils.PIN_DERIVED_EPH_KEY_POP;
        assertThatThrownBy(() -> service.validatePinDerivedEphKeyPopTokenRequest(session, pinDerivedEphKeyPop))
                .isInstanceOf(InvalidRequestException.class);
    }

    @DisplayName("validate device_key_pop session_id expired")
    @Test
    void test006() {
        doReturn(NONCE_EXPIRED).when(session).getCheckedParameterAsInstant(SessionKey.PID_ISSUER_SESSION_ID_EXP_TIME);
        doReturn(NONCE).when(session).getCheckedParameter(SessionKey.PID_ISSUER_SESSION_ID);

        var deviceKeyPop = TestUtils.DEVICE_KEY_POP;
        assertThatThrownBy(() -> service.validateDeviceKeyPopTokenRequest(session, deviceKeyPop))
                .isInstanceOf(InvalidRequestException.class);
    }

    @DisplayName("validate pin_derived_eph_key_pop nonce invalid")
    @Test
    void test007() {
        doReturn(NONCE_EXPIRATION).when(session).getCheckedParameterAsInstant(SessionKey.C_NONCE_EXP_TIME);
        doReturn(NONCE_INVALID).when(session).getCheckedParameter(SessionKey.C_NONCE);

        var pinDerivedEphKeyPop = TestUtils.PIN_DERIVED_EPH_KEY_POP;
        assertThatThrownBy(() -> service.validatePinDerivedEphKeyPop(session, pinDerivedEphKeyPop))
                .isInstanceOf(InvalidRequestException.class);
    }

    @DisplayName("validate pin_derived_eph_key_pop from token request session_id invalid")
    @Test
    void test008() {
        doReturn(NONCE_EXPIRATION).when(session).getCheckedParameterAsInstant(SessionKey.PID_ISSUER_SESSION_ID_EXP_TIME);
        doReturn(NONCE_INVALID).when(session).getCheckedParameter(SessionKey.PID_ISSUER_SESSION_ID);

        var pinDerivedEphKeyPop = TestUtils.PIN_DERIVED_EPH_KEY_POP;
        assertThatThrownBy(() -> service.validatePinDerivedEphKeyPopTokenRequest(session, pinDerivedEphKeyPop))
                .isInstanceOf(InvalidRequestException.class);
    }

    @DisplayName("validate device_key_pop session_id invalid")
    @Test
    void test009() {
        doReturn(NONCE_EXPIRATION).when(session).getCheckedParameterAsInstant(SessionKey.PID_ISSUER_SESSION_ID_EXP_TIME);
        doReturn(NONCE_INVALID).when(session).getCheckedParameter(SessionKey.PID_ISSUER_SESSION_ID);

        var deviceKeyPop = TestUtils.DEVICE_KEY_POP;
        assertThatThrownBy(() -> service.validateDeviceKeyPopTokenRequest(session, deviceKeyPop))
                .isInstanceOf(InvalidRequestException.class);
    }

    @DisplayName("validate pin_derived_eph_key_pop invalid claim audience")
    @Test
    void test010() {
        doReturn(FlowVariant.B1).when(session).getFlowVariant();
        doReturn(AUDIENCE_INVALID).when(config).getCredentialIssuerIdentifier(any());
        doReturn(NONCE_EXPIRATION).when(session).getCheckedParameterAsInstant(SessionKey.C_NONCE_EXP_TIME);
        doReturn(NONCE).when(session).getCheckedParameter(SessionKey.C_NONCE);

        var pinDerivedEphKeyPop = TestUtils.PIN_DERIVED_EPH_KEY_POP;
        assertThatThrownBy(() -> service.validatePinDerivedEphKeyPop(session, pinDerivedEphKeyPop))
                .isInstanceOf(InvalidRequestException.class);
    }

    @DisplayName("validate pin_derived_eph_key_pop from token request invalid claim audience")
    @Test
    void test011() {
        doReturn(FlowVariant.B1).when(session).getFlowVariant();
        doReturn(AUDIENCE_INVALID).when(config).getCredentialIssuerIdentifier(any());
        doReturn(NONCE_EXPIRATION).when(session).getCheckedParameterAsInstant(SessionKey.PID_ISSUER_SESSION_ID_EXP_TIME);
        doReturn(NONCE).when(session).getCheckedParameter(SessionKey.PID_ISSUER_SESSION_ID);

        var pinDerivedEphKeyPop = TestUtils.PIN_DERIVED_EPH_KEY_POP;
        assertThatThrownBy(() -> service.validatePinDerivedEphKeyPopTokenRequest(session, pinDerivedEphKeyPop))
                .isInstanceOf(InvalidRequestException.class);
    }

    @DisplayName("validate device_key_pop invalid claim audience")
    @Test
    void test012() {
        doReturn(FlowVariant.B1).when(session).getFlowVariant();
        doReturn(AUDIENCE_INVALID).when(config).getCredentialIssuerIdentifier(any());
        doReturn(NONCE_EXPIRATION).when(session).getCheckedParameterAsInstant(SessionKey.PID_ISSUER_SESSION_ID_EXP_TIME);
        doReturn(NONCE).when(session).getCheckedParameter(SessionKey.PID_ISSUER_SESSION_ID);

        var deviceKeyPop = TestUtils.DEVICE_KEY_POP;
        assertThatThrownBy(() -> service.validateDeviceKeyPopTokenRequest(session, deviceKeyPop))
                .isInstanceOf(InvalidRequestException.class);
    }
}