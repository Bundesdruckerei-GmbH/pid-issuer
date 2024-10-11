/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import com.fasterxml.jackson.databind.JsonNode;
import de.bdr.openid4vc.vci.data.TokenType;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class TokenHandlerTest {

    @Mock
    private HttpRequest<?> httpRequest;

    private TokenHandler tokenHandler;

    @BeforeEach
    public void setup() {
        tokenHandler = new TokenHandler(Duration.ofMinutes(60), TokenType.DPOP.getValue());
    }

    @Test
    void shouldProcessAccessTokenRequest() {
        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.putParameter(SessionKey.AUTHORIZATION_CODE_EXP_TIME, Instant.now().plusSeconds(5));
        WResponseBuilder responseBuilder = new WResponseBuilder();

        assertDoesNotThrow(() -> tokenHandler.processTokenRequest(httpRequest, responseBuilder, session));
        assertThat(session.containsParameter(SessionKey.ACCESS_TOKEN), is(true));
        assertThat(session.containsParameter(SessionKey.ACCESS_TOKEN_EXP_TIME), is(true));

        assertThat(session.containsParameter(SessionKey.AUTHORIZATION_CODE_EXP_TIME), is(true));

        ResponseEntity<JsonNode> jsonResponse = responseBuilder.buildJSONResponseEntity();
        JsonNode body = jsonResponse.getBody();
        assertNotNull(body);
        assertThat(body.findValuesAsText("token_type").contains(TokenType.DPOP.getValue()), is(true));
        assertThat(body.get("access_token"), notNullValue(JsonNode.class));
        assertThat(body.get("expires_in"), notNullValue(JsonNode.class));
    }

    @Test
    void shouldProcessRefreshTokenRequest() {
        WSession session = new WSessionImpl(FlowVariant.C1, TestUtils.randomSessionId());
        session.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.DEVICE_PUBLIC_KEY.toJSONString());
        WResponseBuilder responseBuilder = new WResponseBuilder();

        assertDoesNotThrow(() -> tokenHandler.processRefreshTokenRequest(httpRequest, responseBuilder, session));
        assertThat(session.containsParameter(SessionKey.ACCESS_TOKEN), is(true));
        assertThat(session.containsParameter(SessionKey.ACCESS_TOKEN_EXP_TIME), is(true));

        ResponseEntity<JsonNode> jsonResponse = responseBuilder.buildJSONResponseEntity();
        JsonNode body = jsonResponse.getBody();
        assertNotNull(body);
        assertThat(body.findValuesAsText("token_type").contains(TokenType.DPOP.getValue()), is(true));
        assertThat(body.get("access_token"), notNullValue(JsonNode.class));
        assertThat(body.get("expires_in"), notNullValue(JsonNode.class));
    }
}
