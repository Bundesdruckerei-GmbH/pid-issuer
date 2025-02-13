/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;


import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.out.identification.IdentificationApi;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationHandlerTest {
    public static Stream<Arguments> provider() {
        return Stream.of(
                Arguments.arguments("http://localhost:8080", FlowVariant.C, "http://localhost:8080/c/finish-authorization?issuer_state="),
                Arguments.arguments("http://localhost:8080", FlowVariant.C1, "http://localhost:8080/c1/finish-authorization?issuer_state="),
                Arguments.arguments("https://pidi.bdr.de", FlowVariant.C, "https://pidi.bdr.de/c/finish-authorization?issuer_state=")
        );
    }

    @DisplayName("Verify result url and issuer_state in session")
    @ParameterizedTest
    @MethodSource("provider")
    void test001(String hostname, FlowVariant variant, String resultUrlPrefix) throws MalformedURLException {
        var identificationProvider = Mockito.mock(IdentificationApi.class);
        var samlRedirectUrl = "https://saml.request.io/path?SAMLRequest=abcde";
        when(identificationProvider.startIdentificationProcess(any(), anyString(), anyString())).
                thenReturn(URI.create(samlRedirectUrl).toURL());
        var authorizationHandlerUT = new AuthorizationHandler(URI.create(hostname).toURL(), identificationProvider);
        long sessionId = TestUtils.randomSessionId();
        WSession session = new WSessionImpl(variant, sessionId);

        WResponseBuilder responseBuilder = new WResponseBuilder();
        authorizationHandlerUT.processAuthRequest(mock(HttpRequest.class), responseBuilder, session, true);

        String issuerState = session.getParameter(SessionKey.ISSUER_STATE);
        var response = responseBuilder.buildStringResponseEntity();
        assertThat(issuerState, is(notNullValue()));
        assertThat(response.getStatusCode().value(), is(303));
        Mockito.verify(identificationProvider)
                .startIdentificationProcess(URI.create(resultUrlPrefix + issuerState).toURL(), issuerState, String.valueOf(sessionId));
        assertThat(response.getHeaders().getFirst("Location"), is(samlRedirectUrl));
    }
}
