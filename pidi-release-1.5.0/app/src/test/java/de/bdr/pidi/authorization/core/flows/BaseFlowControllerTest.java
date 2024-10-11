/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.flows;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.SessionManager;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.FinishAuthException;
import de.bdr.pidi.authorization.core.particle.IdentificationFailedException;
import de.bdr.pidi.base.PidServerException;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static de.bdr.pidi.authorization.ConfigTestData.AUTH_CONFIG;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class BaseFlowControllerTest {

    @Mock
    HttpRequest<?> request;
    @Mock
    WSessionImpl session;
    @Mock
    SessionManager sm;

    MyBaseFlowController out;

    @BeforeEach
    void setup() {
        this.out = new MyBaseFlowController(sm, AUTH_CONFIG);
        Mockito.when(sm.loadByIssuerState(any(), eq(FlowVariant.C))).thenReturn(session);
    }

    @DisplayName("finishAuthorization OidException")
    @Test
    void test001() {
        var clientUrl = "http://localhost:8080/redirect";
        doReturn(clientUrl).when(session).getParameter(SessionKey.REDIRECT_URI);
        out.exception = new IdentificationFailedException("user aborted process");

        assertThatThrownBy(() -> out.processFinishAuthRequest(request))
                .asInstanceOf(InstanceOfAssertFactories.type(FinishAuthException.class))
                .extracting(FinishAuthException::getRedirectUri, FinishAuthException::getState)
                .contains(clientUrl, (Object) null);
    }

    @DisplayName("finishAuthorization PidServerException")
    @Test
    void test002() {
        var clientUrl = "http://localhost:8080/redirect";
        var state = "abc321TUV";
        doReturn(clientUrl).when(session).getParameter(SessionKey.REDIRECT_URI);
        doReturn(state).when(session).getParameter(SessionKey.STATE);
        out.exception = new PidServerException("too bad", new ClassCastException("foo"));

        assertThatThrownBy(() -> out.processFinishAuthRequest(request))
                .asInstanceOf(InstanceOfAssertFactories.type(FinishAuthException.class))
                .extracting(FinishAuthException::getRedirectUri, FinishAuthException::getState)
                .contains(clientUrl, state);
    }

    static class MyBaseFlowController
        extends BaseFlowController {

        RuntimeException exception;

        protected MyBaseFlowController(SessionManager sm, AuthorizationConfiguration configuration) {
            super(sm, configuration, Collections.emptyList(), FlowVariant.C);
        }

        @Override
        protected void doProcessFinishAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
            if (exception != null) {
                throw exception;
            }
        }
    }

}
