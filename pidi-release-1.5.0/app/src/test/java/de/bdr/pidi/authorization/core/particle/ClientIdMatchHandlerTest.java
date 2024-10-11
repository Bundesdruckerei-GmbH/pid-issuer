/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.exception.ValidationFailedException;
import de.bdr.pidi.clientconfiguration.ClientConfigurationService;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientIdMatchHandlerTest {

    private ClientConfigurationService clientConfigurationServiceMock;
    private ClientIdMatchHandler handlerUt;

    @BeforeEach
    void setUp() {
        clientConfigurationServiceMock = mock(ClientConfigurationService.class);
        handlerUt = new ClientIdMatchHandler(clientConfigurationServiceMock);
    }

    @DisplayName("Verify exception when missing client_id parameter")
    @Test
    void test001() {
        HttpRequest<?> request = RequestUtil.getHttpRequest(new HashMap<>());
        WResponseBuilder responseBuilder = new WResponseBuilder();
        WSessionImpl session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());

        var exception = assertThrows(ValidationFailedException.class, () ->
                handlerUt.processPushedAuthRequest(request, responseBuilder, session));

        assertAll(
                () -> assertThat(exception.getMessage(), is("Missing required parameter 'client_id'")),
                () -> assertThat(exception.getLogMessage(), is("Missing required parameter 'client_id'"))
        );
    }

    @DisplayName("Verify exception when null value client_id parameter")
    @Test
    void test002() {
        var parameter = new HashMap<String, String>();
        parameter.put("client_id", null);
        HttpRequest<?> request = RequestUtil.getHttpRequest(parameter);
        WResponseBuilder responseBuilder = new WResponseBuilder();
        WSessionImpl session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());

        var exception = assertThrows(ValidationFailedException.class, () ->
                handlerUt.processPushedAuthRequest(request, responseBuilder, session));

        assertAll(
                () -> assertThat(exception.getMessage(), is("Invalid client id")),
                () -> assertThat(exception.getLogMessage(), is("client id must not be empty"))
        );
    }

    @DisplayName("Verify exception when empty client_id parameter")
    @Test
    void test003() {
        HttpRequest<?> request = RequestUtil.getHttpRequest(Map.of("client_id", ""));
        WResponseBuilder responseBuilder = new WResponseBuilder();
        WSessionImpl session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());

        var exception = assertThrows(ValidationFailedException.class, () ->
                handlerUt.processPushedAuthRequest(request, responseBuilder, session));

        assertAll(
                () -> assertThat(exception.getMessage(), is("Invalid client id")),
                () -> assertThat(exception.getLogMessage(), is("client id must not be empty"))
        );
    }

    @DisplayName("Verify exception when non uuid client_id parameter")
    @Test
    void test004() {
        HttpRequest<?> request = RequestUtil.getHttpRequest(Map.of("client_id", "This should be an uuid"));
        WResponseBuilder responseBuilder = new WResponseBuilder();
        WSessionImpl session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());

        var exception = assertThrows(ValidationFailedException.class, () ->
                handlerUt.processPushedAuthRequest(request, responseBuilder, session));

        assertAll(
                () -> assertThat(exception.getMessage(), is("Invalid client id")),
                () -> assertThat(exception.getLogMessage(), is("client id must be a valid UUID"))
        );
    }

    @DisplayName("Verify exception when not registered client_id parameter")
    @Test
    void test005() {
        UUID clientId = UUID.randomUUID();
        when(clientConfigurationServiceMock.isValidClientId(clientId)).thenReturn(false);
        HttpRequest<?> request = RequestUtil.getHttpRequest(Map.of("client_id", clientId.toString()));
        WResponseBuilder responseBuilder = new WResponseBuilder();
        WSessionImpl session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());

        var exception = assertThrows(ClientNotRegisteredException.class, () ->
                handlerUt.processPushedAuthRequest(request, responseBuilder, session));

        assertAll(
                () -> assertThat(exception.getMessage(), is("Client Id not registered: " + clientId)),
                () -> assertThat(exception.getLogMessage(), is("Client Id not registered: " + clientId))
        );
    }

    @DisplayName("Verify call on ClientConfigurationService when registered client_id parameter")
    @Test
    void test006() {
        UUID clientId = UUID.randomUUID();
        when(clientConfigurationServiceMock.isValidClientId(clientId)).thenReturn(true);

        HttpRequest<?> request = RequestUtil.getHttpRequest(Map.of("client_id", clientId.toString()));
        WResponseBuilder responseBuilder = new WResponseBuilder();
        WSessionImpl session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());

        handlerUt.processPushedAuthRequest(request, responseBuilder, session);

        verify(clientConfigurationServiceMock, times(1)).isValidClientId(clientId);

        assertThat(session.getParameter(SessionKey.CLIENT_ID), is(clientId.toString()));
    }

    @DisplayName("Verify client_id match on auth request")
    @Test
    void test007() {
        UUID clientId = UUID.randomUUID();
        when(clientConfigurationServiceMock.isValidClientId(clientId)).thenReturn(true);

        HttpRequest<?> request = RequestUtil.getHttpRequest(Map.of("client_id", clientId.toString()));
        WResponseBuilder responseBuilder = new WResponseBuilder();
        WSessionImpl session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.putParameter(SessionKey.CLIENT_ID, clientId.toString());

        handlerUt.processAuthRequest(request, responseBuilder, session, true);

        verify(clientConfigurationServiceMock, times(1)).isValidClientId(clientId);

    }

    @DisplayName("Verify exception when client_id form session and request do not match on auth request")
    @Test
    void test008() {
        UUID clientIdFromRequest = UUID.randomUUID();
        UUID clientIdFromSession = UUID.randomUUID();

        when(clientConfigurationServiceMock.isValidClientId(clientIdFromRequest)).thenReturn(true);

        HttpRequest<?> request = RequestUtil.getHttpRequest(Map.of("client_id", clientIdFromRequest.toString()));
        WResponseBuilder responseBuilder = new WResponseBuilder();
        WSessionImpl session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.putParameter(SessionKey.CLIENT_ID, clientIdFromSession.toString());

        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> handlerUt.processAuthRequest(request, responseBuilder, session, true));

        assertAll(
                () -> assertThat(exception.getMessage(), is("client_id parameter from par request doesn't match client_id")),
                () -> assertThat(exception.getLogMessage(), is("client_id parameter from par request doesn't match client_id")));
       }
}
