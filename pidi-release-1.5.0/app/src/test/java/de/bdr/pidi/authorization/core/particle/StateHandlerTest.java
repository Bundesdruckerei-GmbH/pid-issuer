/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.ParameterTooLongException;
import de.bdr.pidi.testdata.TestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StateHandlerTest {
    private final StateHandler stateHandlerUT = new StateHandler();

    @DisplayName("Verify that state parameter is stored in session on par")
    @ParameterizedTest
    @ValueSource(strings = {"", "abc"})
    void test001(String state) {
        HttpRequest<?> requestMock = mock(HttpRequest.class);
        when(requestMock.getParameters()).thenReturn(Map.of("state", state));
        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        WResponseBuilder responseBuilderMoc = mock(WResponseBuilder.class);
        stateHandlerUT.processPushedAuthRequest(requestMock, responseBuilderMoc, session);

        assertThat(session.getParameter(SessionKey.STATE), is(state));
        verifyNoInteractions(responseBuilderMoc);
    }

    @DisplayName("Verify that missing state parameter is not stored in session on par")
    @Test
    void test002() {
        HttpRequest<?> requestMock = mock(HttpRequest.class);
        when(requestMock.getParameters()).thenReturn(Collections.emptyMap());
        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        WResponseBuilder responseBuilderMoc = mock(WResponseBuilder.class);
        stateHandlerUT.processPushedAuthRequest(requestMock, responseBuilderMoc, session);

        assertThat(session.getParameter(SessionKey.STATE), is(nullValue()));
        verifyNoInteractions(responseBuilderMoc);
    }

    @DisplayName("Verify exception on too long state parameter on par")
    @Test
    void test003() {
        String state = RandomStringUtils.random(2049);
        HttpRequest<?> requestMock = mock(HttpRequest.class);
        when(requestMock.getParameters()).thenReturn(Map.of("state", state));
        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        WResponseBuilder responseBuilderMoc = mock(WResponseBuilder.class);
        var exception = assertThrows(ParameterTooLongException.class, () -> stateHandlerUT.processPushedAuthRequest(requestMock, responseBuilderMoc, session));

        assertThat(session.getParameter(SessionKey.STATE), is(nullValue()));
        assertThat(exception.getErrorCode(), is("invalid_request"));
        assertThat(exception.getMessage(), is("The state parameter exceeds the maximum permitted size of 2048 bytes"));
        verifyNoInteractions(responseBuilderMoc);
    }
}
