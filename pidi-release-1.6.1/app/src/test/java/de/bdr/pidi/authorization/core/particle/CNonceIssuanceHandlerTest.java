/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.NonceFactory;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.Nonce;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class CNonceIssuanceHandlerTest {

    private static final String SECURE_RANDOM_NONCE = "1234567890123456789012";

    private final Duration accessTokenLifetime = Duration.ofMinutes(1);
    private final CNonceIssuanceHandler nonceIssuanceHandler = new CNonceIssuanceHandler(accessTokenLifetime);

    @DisplayName("Verify session and response values are set on token request")
    @Test
    void test001() {
        var request = RequestUtil.getHttpRequest(Collections.emptyMap());
        var responseBuilder = new WResponseBuilder();
        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        try (MockedStatic<NonceFactory> nonceFactoryMock = mockStatic(NonceFactory.class)) {
            nonceFactoryMock.when(() -> NonceFactory.createSecureRandomNonce(accessTokenLifetime))
                    .thenReturn(new Nonce(SECURE_RANDOM_NONCE, accessTokenLifetime));

            nonceIssuanceHandler.processTokenRequest(request, responseBuilder, session);

            assertThat(session.getParameter(SessionKey.C_NONCE)).isEqualTo(SECURE_RANDOM_NONCE);
            var expirationTime = session.getParameterAsInstant(SessionKey.C_NONCE_EXP_TIME);
            var expectedExpiration = Instant.now().plus(accessTokenLifetime);
            assertThat(expirationTime).isCloseTo(expectedExpiration, within(5, ChronoUnit.SECONDS));

            var response = responseBuilder.buildJSONResponseEntity();
            assertThat(response.getBody().get("c_nonce").asText()).isEqualTo(SECURE_RANDOM_NONCE);
            assertThat(response.getBody().get("c_nonce_expires_in").asLong()).isEqualTo(accessTokenLifetime.toSeconds());
        }
    }

    @DisplayName("Verify session and response values are set on credential request")
    @Test
    void test002() {
        var request = RequestUtil.getHttpRequest(TestUtils.createMdocCredentialRequest());
        var responseBuilder = new WResponseBuilder();
        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        try(MockedStatic<NonceFactory> nonceFactoryMock = mockStatic(NonceFactory.class)) {
            nonceFactoryMock.when(() -> NonceFactory.createSecureRandomNonce(accessTokenLifetime))
                    .thenReturn(new Nonce(SECURE_RANDOM_NONCE, accessTokenLifetime));

            nonceIssuanceHandler.processCredentialRequest(request, responseBuilder, session);

            assertThat(session.getParameter(SessionKey.C_NONCE)).isEqualTo(SECURE_RANDOM_NONCE);
            var expirationTime = session.getParameterAsInstant(SessionKey.C_NONCE_EXP_TIME);
            var expectedExpiration = Instant.now().plus(accessTokenLifetime);
            assertThat(expirationTime).isCloseTo(expectedExpiration, within(5, ChronoUnit.SECONDS));

            var response = responseBuilder.buildJSONResponseEntity();
            assertThat(response.getBody().get("c_nonce").asText()).isEqualTo(SECURE_RANDOM_NONCE);
            assertThat(response.getBody().get("c_nonce_expires_in").asLong()).isEqualTo(accessTokenLifetime.toSeconds());
        }
    }
}
