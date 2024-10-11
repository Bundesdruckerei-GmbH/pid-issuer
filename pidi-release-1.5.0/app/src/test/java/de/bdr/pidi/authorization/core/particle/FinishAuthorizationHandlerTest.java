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
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.exception.ValidationFailedException;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static de.bdr.pidi.authorization.ConfigTestData.AUTH_CONFIG;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinishAuthorizationHandlerTest {

    @Mock
    private HttpRequest<?> httpRequest;

    private FinishAuthorizationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new FinishAuthorizationHandler(AUTH_CONFIG.getAuthorizationCodeLifetime());
    }

    @Test
    void shouldProcess() {
        String issuerState = RandomUtil.randomString();
        var params = Map.of("issuer_state", issuerState);

        when(httpRequest.getParameters()).thenReturn(params);

        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.putParameter(SessionKey.ISSUER_STATE, issuerState);
        session.putParameter(SessionKey.REDIRECT_URI, "https://secure.redirect.com");
        session.putParameter(SessionKey.STATE, "any Statevalue from Client");
        session.putParameter(SessionKey.IDENTIFICATION_RESULT, "Success");
        WResponseBuilder responseBuilder = new WResponseBuilder();

        handler.processFinishAuthRequest(httpRequest, responseBuilder, session);

        var response = responseBuilder.buildStringResponseEntity();
        assertThat(response.getStatusCode().value(), is(302));
        URI location = response.getHeaders().getLocation();
        assertThat(location, is(notNullValue()));
        assertThat(location.getScheme(), is("https"));
        assertThat(location.getHost(), is("secure.redirect.com"));
        assertThat(location.getQuery(), containsString("code="));
        assertThat(location.getRawQuery(), containsString("state=any%20Statevalue%20from%20Client"));

        assertThat(session.containsParameter(SessionKey.AUTHORIZATION_CODE_EXP_TIME), is(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"88", "a-b"})
    @NullAndEmptySource
    void shouldThrowExceptionWhenIssuerStateIsInvalid(String issuerState) {
        Map<String, String> params;
        if (issuerState == null) {
            params = Collections.emptyMap();
        } else {
            params = Map.of("issuer_state", issuerState);
        }
        when(httpRequest.getParameters()).thenReturn(params);

        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.putParameter(SessionKey.ISSUER_STATE, issuerState);
        WResponseBuilder responseBuilder = new WResponseBuilder();

        ValidationFailedException exception = Assertions.assertThrows(ValidationFailedException.class,
                () -> handler.processFinishAuthRequest(httpRequest, responseBuilder, session));
        if (issuerState == null || issuerState.isEmpty()) {
            assertThat(exception.getLogMessage(), is("Missing required parameter 'issuer_state'"));
        } else {
            assertThat(exception.getLogMessage(), is("Invalid issuer state " + issuerState));
        }
    }

    @Test
    void shouldThrowExceptionWhenIssuerStatesAreDifferent() {
        givenRequestParameterWithIssuerState();

        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        String issuerStateSession = RandomUtil.randomString();
        session.putParameter(SessionKey.ISSUER_STATE, issuerStateSession);
        WResponseBuilder responseBuilder = new WResponseBuilder();

        VerificationFailedException exception = Assertions.assertThrows(VerificationFailedException.class,
                () -> handler.processFinishAuthRequest(httpRequest, responseBuilder, session));
        assertThat(exception.getMessage(), is("Invalid issuer state"));
    }

    private Map<String,String> givenRequestParameterWithIssuerState() {
        var issuerStateParam = RandomUtil.randomString();
        var params = Map.of("issuer_state", issuerStateParam);
        when(httpRequest.getParameters()).thenReturn(params);
        return params;
    }

    @Test
    void shouldThrowExceptionWhenRedirectUriIsMissing() {
        var params = givenRequestParameterWithIssuerState();
        WSession session = givenSessionWithIssuerState(params);
        WResponseBuilder responseBuilder = new WResponseBuilder();

        InvalidRequestException exception = Assertions.assertThrows(InvalidRequestException.class,
                () -> handler.processFinishAuthRequest(httpRequest, responseBuilder, session));
        assertThat(exception.getMessage(), is("Missing redirect uri"));
    }

    private WSession givenSessionWithIssuerState(Map<String, String> params) {
        var issuerState = params.get("issuer_state");

        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.putParameter(SessionKey.ISSUER_STATE, issuerState);
        return session;
    }

    @Test
    void given_noIdentificationResult_when_process_then_throwException() {
        var params = givenRequestParameterWithIssuerState();
        var session = givenSessionWithIssuerState(params);
        session.putParameter(SessionKey.REDIRECT_URI, "https://secure.redirect.com");
        WResponseBuilder responseBuilder = new WResponseBuilder();

        IdentificationFailedException exception = Assertions.assertThrows(IdentificationFailedException.class,
                () -> handler.processFinishAuthRequest(httpRequest, responseBuilder, session));
        assertThat(exception.getMessage(), is("Identification failed"));
    }

    @Test
    void given_identificationResultError_when_process_then_throwException() {
        var params = givenRequestParameterWithIssuerState();
        var session = givenSessionWithIssuerState(params);
        session.putParameter(SessionKey.REDIRECT_URI, "https://secure.redirect.com");
        session.putParameter(SessionKey.IDENTIFICATION_RESULT, "Error");
        var details = "USER_ABORTED";
        session.putParameter(SessionKey.IDENTIFICATION_ERROR, details);
        WResponseBuilder responseBuilder = new WResponseBuilder();

        IdentificationFailedException exception = Assertions.assertThrows(IdentificationFailedException.class,
                () -> handler.processFinishAuthRequest(httpRequest, responseBuilder, session));
        Assertions.assertAll(
                () -> assertThat(exception.getMessage(), is("Identification failed")),
                () -> assertThat(exception.getLogMessage(), is(details))
        );
    }

}
