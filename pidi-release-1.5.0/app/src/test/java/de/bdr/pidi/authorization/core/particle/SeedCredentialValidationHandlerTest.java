/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.PinRetryCounter;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidGrantException;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.service.PidSerializer;
import de.bdr.pidi.authorization.core.service.PinProofService;
import de.bdr.pidi.authorization.core.service.PinRetryCounterService;
import de.bdr.pidi.authorization.core.util.PinUtil;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.SeedException;
import de.bdr.pidi.authorization.out.issuance.SeedPidBuilder;
import de.bdr.pidi.testdata.Pin;
import de.bdr.pidi.testdata.TestUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static de.bdr.pidi.authorization.ConfigTestData.AUTH_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SeedCredentialValidationHandlerTest {
    private static final String ISSUER_IDENTIFIER = AUTH_CONFIG.getCredentialIssuerIdentifier(FlowVariant.B1);
    private static final String ISSUER_ID = "issuerId";
    private static final String SEED_CREDENTIAL = "seeeed";
    private static final SeedPidBuilder.PinSeedData SEED_DATA = new SeedPidBuilder.SeedData(PidCredentialData.Companion.getTEST_DATA_SET(), TestUtils.DEVICE_PUBLIC_KEY, TestUtils.PIN_DERIVED_PUBLIC_KEY, ISSUER_ID, Instant.now(), Instant.now().plusSeconds(30));
    private static final Pin DIFFERNT_PIN = Pin.createPin(TestUtils.C_NONCE);

    private final String pinRetryCounterId = PinUtil.computeRetryCounterId(SEED_DATA.clientInstanceKey());
    private PinRetryCounter pinRetryCounter;

    private static final String PIN_DERIVED_EPH_KEY_POP = TestUtils.PIN_DERIVED_EPH_KEY_POP.serialize();
    private static final String DEVICE_KEY_PROOF = TestUtils.DEVICE_KEY_POP.serialize();

    @Mock
    private SeedPidBuilder seedPidBuilder;
    @Mock
    private PidSerializer pidSerializer;
    @Mock
    private PinRetryCounterService pinRetryService;
    @Mock
    private PinProofService pinProofService;

    @InjectMocks
    private SeedCredentialValidationHandler handler;

    private WSession session;
    private WResponseBuilder response;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        FieldUtils.writeField(handler, "credentialIssuerIdentifier", ISSUER_IDENTIFIER, true);
        session = new WSessionImpl(FlowVariant.B1, 1L);
        session.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.DEVICE_PUBLIC_KEY.toJSONString());
        session.putParameter(SessionKey.PID_ISSUER_SESSION_ID, TestUtils.C_NONCE);
        response = new WResponseBuilder();
        pinRetryCounter = getPinRetryCounter(0);
    }

    @DisplayName("Validate read seed data success")
    @Test
    void test001() {
        var request = RequestUtil.getHttpRequest(Map.of(
                "seed_credential", SEED_CREDENTIAL,
                "pin_derived_eph_key_pop", PIN_DERIVED_EPH_KEY_POP,
                "device_key_pop", DEVICE_KEY_PROOF
        ));
        doReturn(SEED_DATA).when(seedPidBuilder).extractVerifiedPinSeed(SEED_CREDENTIAL, ISSUER_IDENTIFIER);
        doReturn("dummy").when(pidSerializer).toString(SEED_DATA.pidCredentialData());
        doReturn(pinRetryCounter.getDigest()).when(pinRetryService).loadPinCounter(any());
        doReturn(SEED_DATA.clientInstanceKey()).when(pinProofService).validateDeviceKeyPopTokenRequest(any(), any());
        doReturn(SEED_DATA.pinDerivedKey()).when(pinProofService).validatePinDerivedEphKeyPopTokenRequest(any(), any());

        handler.processSeedCredentialTokenRequest(request, response, session);

        assertThat(session.getParameter(SessionKey.IDENTIFICATION_RESULT)).isEqualTo("Success");
        assertThat(session.getParameter(SessionKey.IDENTIFICATION_DATA)).isEqualTo("dummy");
    }

    @DisplayName("Validate InvalidRequestException on missing parameter")
    @Test
    void test002() {
        var incompleteRequest = RequestUtil.getHttpRequest(Map.of(
                "pin_derived_eph_key_pop", PIN_DERIVED_EPH_KEY_POP,
                "device_key_pop", DEVICE_KEY_PROOF
        ));

        assertThatThrownBy(() -> handler.processSeedCredentialTokenRequest(incompleteRequest, response, session))
                .isInstanceOf(InvalidRequestException.class);
    }

    @DisplayName("Validate InvalidRequestException on empty parameter")
    @Test
    void test003() {
        var emptyParamRequest = RequestUtil.getHttpRequest(Map.of(
                "seed_credential", SEED_CREDENTIAL,
                "pin_derived_eph_key_pop", "",
                "device_key_pop", DEVICE_KEY_PROOF
        ));

        assertThatThrownBy(() -> handler.processSeedCredentialTokenRequest(emptyParamRequest, response, session))
                .isInstanceOf(InvalidRequestException.class);
    }

    @DisplayName("Validate InvalidGrantException on invalid seed credential")
    @Test
    void test004() {
        var request = getValidRequest();
        doThrow(new SeedException(SeedException.Kind.INVALID, "")).when(seedPidBuilder).extractVerifiedPinSeed(SEED_CREDENTIAL, ISSUER_IDENTIFIER);

        assertThatThrownBy(() -> handler.processSeedCredentialTokenRequest(request, response, session))
                .isInstanceOf(InvalidGrantException.class);
    }

    @DisplayName("Validate InvalidRequestException on non parsable jws")
    @Test
    void test005() {
        var invalidRequest = RequestUtil.getHttpRequest(Map.of(
                "seed_credential", SEED_CREDENTIAL,
                "pin_derived_eph_key_pop", "nonParsable",
                "device_key_pop", DEVICE_KEY_PROOF
        ));

        assertThatThrownBy(() -> handler.processSeedCredentialTokenRequest(invalidRequest, response, session))
                .isInstanceOf(InvalidRequestException.class);
    }

    @DisplayName("Validate InvalidGrantException on wrong client instance key")
    @Test
    void test006() {
        var request = getValidRequest();
        doReturn(SEED_DATA).when(seedPidBuilder).extractVerifiedPinSeed(SEED_CREDENTIAL, ISSUER_IDENTIFIER);
        var differentDpopJwk = TestUtils.DIFFERENT_KEY_PAIR.toPublicJWK().toJSONString();
        session.putParameter(SessionKey.DPOP_PUBLIC_KEY, differentDpopJwk);

        assertThatThrownBy(() -> handler.processSeedCredentialTokenRequest(request, response, session))
                .hasMessage("Seed credential invalid")
                .isInstanceOf(InvalidGrantException.class);
    }

    @DisplayName("Validate InvalidGrantException on wrong pin")
    @Test
    void test008() {
        var wrongPinRequest = getWrongPinRequest();
        doReturn(SEED_DATA).when(seedPidBuilder).extractVerifiedPinSeed(SEED_CREDENTIAL, ISSUER_IDENTIFIER);
        doReturn(pinRetryCounter.getDigest()).when(pinRetryService).loadPinCounter(any());
        doReturn(SEED_DATA.clientInstanceKey()).when(pinProofService).validateDeviceKeyPopTokenRequest(any(), any());
        doReturn(DIFFERNT_PIN.pinDerivedEphKeyPop().getHeader().getJWK()).when(pinProofService).validatePinDerivedEphKeyPopTokenRequest(any(), any());

        assertThatThrownBy(() -> handler.processSeedCredentialTokenRequest(wrongPinRequest, response, session))
                .hasMessage("PIN invalid")
                .isInstanceOf(InvalidGrantException.class);
        verify(pinRetryService).increment(argThat(p -> p.equals(pinRetryCounter.getDigest())), any());
    }

    @DisplayName("Validate InvalidGrantException on wrong session id")
    @Test
    void test010() {
        var request = getValidRequest();
        doReturn(SEED_DATA).when(seedPidBuilder).extractVerifiedPinSeed(SEED_CREDENTIAL, ISSUER_IDENTIFIER);
        doReturn(pinRetryCounter.getDigest()).when(pinRetryService).loadPinCounter(any());
        var differentSessionId = RandomUtil.randomString();
        session.putParameter(SessionKey.PID_ISSUER_SESSION_ID, differentSessionId);

        assertThatThrownBy(() -> handler.processSeedCredentialTokenRequest(request, response, session))
                .hasMessage("PIN invalid")
                .isInstanceOf(InvalidGrantException.class);
        verify(pinRetryService).increment(argThat(p -> p.equals(pinRetryCounter.getDigest())), any());
    }

    private PinRetryCounter getPinRetryCounter(int count) {
        return new PinRetryCounter(1L, pinRetryCounterId, count, Instant.now().plusSeconds(30));
    }

    private HttpRequest<?> getValidRequest() {
        return RequestUtil.getHttpRequest(Map.of(
                "seed_credential", SEED_CREDENTIAL,
                "pin_derived_eph_key_pop", PIN_DERIVED_EPH_KEY_POP,
                "device_key_pop", DEVICE_KEY_PROOF
        ));
    }

    private HttpRequest<?> getWrongPinRequest() {
        return RequestUtil.getHttpRequest(Map.of(
                "seed_credential", SEED_CREDENTIAL,
                "pin_derived_eph_key_pop", DIFFERNT_PIN.pinDerivedEphKeyPop().serialize(),
                "device_key_pop", DIFFERNT_PIN.deviceKeyPop().serialize()
        ));
    }
}