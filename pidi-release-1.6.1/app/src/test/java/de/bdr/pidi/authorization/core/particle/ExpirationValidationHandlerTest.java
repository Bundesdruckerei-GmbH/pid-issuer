/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.data.TokenType;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidGrantException;
import de.bdr.pidi.authorization.core.exception.UnauthorizedException;
import de.bdr.pidi.base.PidServerException;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ExpirationValidationHandlerTest {

    @Mock
    private HttpRequest<?> httpRequest;
    @Mock
    private HttpRequest<CredentialRequest> mockedRequest;

    private final ExpirationValidationHandler handler = new ExpirationValidationHandler(TokenType.DPOP.getValue());

    @Test
    void shouldThrowExceptionWhenCodeExpirationTimeIsInvalid() {
        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.putParameter(SessionKey.AUTHORIZATION_CODE_EXP_TIME, (Instant)null);
        WResponseBuilder responseBuilder = new WResponseBuilder();

        assertThrows(PidServerException.class, () -> handler.processTokenRequest(httpRequest, responseBuilder, session));
    }

    @Test
    void shouldThrowExceptionOnExpiredCode() {
        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.putParameter(SessionKey.AUTHORIZATION_CODE_EXP_TIME, Instant.now().minusSeconds(10));
        WResponseBuilder responseBuilder = new WResponseBuilder();

        InvalidGrantException exception = assertThrows(InvalidGrantException.class, () -> handler.processTokenRequest(httpRequest, responseBuilder, session));
        assertThat(exception.getLogMessage(), is("Session is expired"));
    }

    @Test
    void shouldThrowExceptionOnExpiredToken() {
        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        WResponseBuilder responseBuilder = new WResponseBuilder();

        session.putParameter(SessionKey.ACCESS_TOKEN_EXP_TIME, Instant.now().minusSeconds(10));

        assertThrows(UnauthorizedException.class,
                () -> handler.processCredentialRequest(mockedRequest, responseBuilder, session));
    }
}