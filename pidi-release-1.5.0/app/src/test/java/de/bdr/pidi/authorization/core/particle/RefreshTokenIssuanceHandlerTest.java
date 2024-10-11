/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import com.nimbusds.oauth2.sdk.ParseException;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.service.PidSerializer;
import de.bdr.pidi.authorization.core.service.ServiceTestData;
import de.bdr.pidi.authorization.out.issuance.SeedPidBuilder;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class RefreshTokenIssuanceHandlerTest {

    @Mock
    private SeedPidBuilder seedPidBuilder;

    @Mock
    private PidSerializer pidSerializer;

    @Mock
    HttpRequest<?> request;

    private RefreshTokenIssuanceHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RefreshTokenIssuanceHandler(seedPidBuilder, pidSerializer, "dummy/c1/");
    }

    @Test
    @DisplayName("Verify refresh token issuance on token request is successful")
    void test001() throws ParseException {
        WSession session = new WSessionImpl(FlowVariant.C1, 1L);
        session.putParameter(SessionKey.IDENTIFICATION_DATA, ServiceTestData.createPidString());
        session.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.DEVICE_PUBLIC_KEY.toJSONString());
        doReturn(ServiceTestData.createPid()).when(pidSerializer).fromString(anyString());
        doReturn("seedPid").when(seedPidBuilder).build(any(), any(), anyString());
        var responseBuilder = new WResponseBuilder();

        assertDoesNotThrow(() -> handler.processTokenRequest(request, responseBuilder, session));

        var body = responseBuilder.buildJSONResponseEntity().getBody();
        assertThat(body).isNotNull();
        assertThat(body.has("refresh_token")).isTrue();
        assertThat(body.get("refresh_token").asText()).isEqualTo("seedPid");
    }
}