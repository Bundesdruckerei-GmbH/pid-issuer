/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.service;

import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.NonceFactory;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.Nonce;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static de.bdr.pidi.authorization.ConfigTestData.AUTH_CONFIG;
import static java.util.Optional.of;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class NonceServiceImplTest {

    @Mock
    private NonceFactory nonceFactory;
    @Mock
    private WSession session;
    @Spy
    private AuthorizationConfiguration authConfig = AUTH_CONFIG;
    @InjectMocks
    private NonceServiceImpl out;

    private final Duration dpopLifetime = AUTH_CONFIG.getDpopNonceLifetime();

    @Test
    void when_generateAndStoreDpopNonce_then_ok() {
        var nonce = new Nonce("nonce_for_service", dpopLifetime);
        try (MockedStatic<NonceFactory> nonceFactoryMock = mockStatic(NonceFactory.class)) {
            nonceFactoryMock.when(() -> NonceFactory.createSecureRandomNonce(dpopLifetime))
                    .thenReturn(nonce);

            var result = this.out.generateAndStoreDpopNonce(session);

            Assertions.assertAll(
                    () -> Assertions.assertEquals(nonce, result),
                    () -> Mockito.verify(session).putParameter(SessionKey.DPOP_NONCE, nonce.nonce()),
                    () -> Mockito.verify(session).putParameter(SessionKey.DPOP_NONCE_EXP_TIME, nonce.expirationTime())
            );
        }
    }

    @Test
    void when_fetchDpopNonceFromSession_then_ok() {
        var value = "diesIstDerNonceWert";
        Mockito.when(session.getOptionalParameter(SessionKey.DPOP_NONCE)).thenReturn(of(value));
        var exp = Instant.now().plusSeconds(17);
        Mockito.when(session.getCheckedParameterAsInstant(SessionKey.DPOP_NONCE_EXP_TIME)).thenReturn(exp);

        var result = this.out.fetchDpopNonceFromSession(session);

        Assertions.assertAll(
                () -> Assertions.assertEquals(value, result.nonce()),
                () -> Assertions.assertEquals(exp, result.expirationTime())
        );
    }
}
