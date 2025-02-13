/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import com.nimbusds.oauth2.sdk.GrantType;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidGrantException;
import de.bdr.pidi.authorization.core.service.PidSerializer;
import de.bdr.pidi.authorization.out.issuance.SeedException;
import de.bdr.pidi.authorization.out.issuance.SeedPidBuilder;
import de.bdr.pidi.testdata.TestUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenValidationHandlerTest {
    @Mock
    private SeedPidBuilder seedPidBuilder;
    @Mock
    private PidSerializer pidSerializer;
    @Mock
    private HttpRequest<?> httpRequest;

    @InjectMocks
    private RefreshTokenValidationHandler handler;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        FieldUtils.writeField(handler, "credentialIssuerIdentifier", "issuerId", true);
    }

    @Test
    void shouldProcessRefreshTokenRequest() {
        var params = Map.of("grant_type", GrantType.REFRESH_TOKEN.getValue(), "refresh_token", "refresh_token");
        when(httpRequest.getParameters()).thenReturn(params);

        WSession session = new WSessionImpl(FlowVariant.C1, TestUtils.randomSessionId());
        session.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.DEVICE_PUBLIC_KEY.toJSONString());
        WResponseBuilder responseBuilder = new WResponseBuilder();
        when(seedPidBuilder.extractVerifiedEncSeed("refresh_token", "issuerId"))
                .thenReturn(new SeedPidBuilder.SeedData(null, TestUtils.DEVICE_PUBLIC_KEY, "issuerId", null, null));

        assertDoesNotThrow(() -> handler.processRefreshTokenRequest(httpRequest, responseBuilder, session));
        MatcherAssert.assertThat(session.containsParameter(SessionKey.ACCESS_TOKEN), is(false));
        MatcherAssert.assertThat(session.containsParameter(SessionKey.ACCESS_TOKEN_EXP_TIME), is(false));
    }

    @Test
    void shouldThrowExceptionWhenJwksAreDifferent() {
        var params = Map.of("grant_type", GrantType.REFRESH_TOKEN.getValue(), "refresh_token", "refresh_token");
        when(httpRequest.getParameters()).thenReturn(params);

        WSession session = new WSessionImpl(FlowVariant.C1, TestUtils.randomSessionId());
        session.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.DEVICE_PUBLIC_KEY.toJSONString());
        WResponseBuilder responseBuilder = new WResponseBuilder();
        when(seedPidBuilder.extractVerifiedEncSeed(eq("refresh_token"), anyString())).thenReturn(new SeedPidBuilder.SeedData(null, TestUtils.CLIENT_PUBLIC_KEY, "issuerId", null, null));

        InvalidGrantException exception = assertThrows(InvalidGrantException.class, () -> handler.processRefreshTokenRequest(httpRequest, responseBuilder, session));
        MatcherAssert.assertThat(exception.getMessage(), is("Key mismatch"));
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenIsInvalid() {
        var params = Map.of("grant_type", GrantType.REFRESH_TOKEN.getValue(), "refresh_token", "refresh_token");
        when(httpRequest.getParameters()).thenReturn(params);

        WSession session = new WSessionImpl(FlowVariant.C1, TestUtils.randomSessionId());
        session.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.DEVICE_PUBLIC_KEY.toJSONString());
        WResponseBuilder responseBuilder = new WResponseBuilder();
        SeedException seedException = new SeedException(SeedException.Kind.INVALID, "token invalid");
        when(seedPidBuilder.extractVerifiedEncSeed(eq("refresh_token"), anyString())).thenThrow(seedException);

        InvalidGrantException exception = assertThrows(InvalidGrantException.class, () -> handler.processRefreshTokenRequest(httpRequest, responseBuilder, session));
        MatcherAssert.assertThat(exception.getMessage(), is("Refresh token invalid"));
        MatcherAssert.assertThat(exception.getCause(), is(seedException));
    }

}