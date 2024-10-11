/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.service.KeyProofService;
import de.bdr.pidi.authorization.core.service.PinProofService;
import de.bdr.pidi.authorization.core.service.PinRetryCounterService;
import de.bdr.pidi.base.requests.SeedCredentialRequest;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class InitPinRetryCounterHandlerTest {
    private static final Instant NOW = Instant.now();
    private static final Instant PIN_NONCE_EXP_TIME = NOW.plusSeconds(60L);

    @Mock
    private PinRetryCounterService pinRetryCounterService;

    @Mock
    private PinProofService pinProofService;

    @Mock
    private KeyProofService keyProofService;

    @InjectMocks
    private InitPinRetryCounterHandler handler;

    private HttpRequest<SeedCredentialRequest> request;
    private WResponseBuilder responseBuilder;
    private WSessionImpl session;

    @BeforeEach
    void setUp() {
        request = RequestUtil.getHttpRequest(TestUtils.createSeedCredentialRequest());
        responseBuilder = new WResponseBuilder();
        session = new WSessionImpl(FlowVariant.B1, TestUtils.randomSessionId());
        session.putParameter(SessionKey.C_NONCE_EXP_TIME, PIN_NONCE_EXP_TIME);
        session.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.DEVICE_PUBLIC_KEY.toJSONString());
        session.putParameter(SessionKey.C_NONCE, TestUtils.C_NONCE);
        doReturn(TestUtils.DEVICE_PUBLIC_KEY).when(keyProofService).validateJwtProof(any(), any(), anyBoolean());
        doReturn(TestUtils.PIN_DERIVED_PUBLIC_KEY).when(pinProofService).validatePinDerivedEphKeyPop(any(), any());
    }

    @Test
    void testSuccess() {
        assertDoesNotThrow(() -> handler.processSeedCredentialRequest(request, responseBuilder, session));
        Mockito.verify(pinRetryCounterService).initPinRetryCounter(TestUtils.DEVICE_PUBLIC_KEY);
        assertThat(session.getCheckedParameterAsJwk(SessionKey.PIN_DERIVED_PUBLIC_KEY)).isNotNull();
    }
}