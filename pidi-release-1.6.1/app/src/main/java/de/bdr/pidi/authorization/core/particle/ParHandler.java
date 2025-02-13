/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.exception.ValidationFailedException;
import de.bdr.pidi.authorization.core.util.RandomUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static de.bdr.pidi.authorization.core.exception.InvalidRequestException.missingParameter;

public class ParHandler implements OidHandler {
    private static final String RESPONSE_TYPE = "response_type";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Duration requestUriLifetime;

    /**
     * @param requestUriLifetime the lifetime of a request uri
     */
    public ParHandler(Duration requestUriLifetime) {
        this.requestUriLifetime = requestUriLifetime;
    }

    @Override
    public void processPushedAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        validatePARParams(request.getParameters());

        String requestUri = createRequestUri();

        session.putParameter(SessionKey.REQUEST_URI, requestUri);
        var expires = Instant.now().plus(requestUriLifetime);
        session.putParameter(SessionKey.REQUEST_URI_EXP_TIME, expires);

        ObjectNode body = objectMapper.createObjectNode()
                .put("request_uri", requestUri)
                .put("expires_in", requestUriLifetime.toSeconds());

        response.withHttpStatus(201)
                .withJsonBody(body);
    }

    private String createRequestUri() {
        return "urn:ietf:params:oauth:request_uri:" + RandomUtil.randomString();
    }

    private void validatePARParams(Map<String, String> parameters) {
        if (!parameters.containsKey(RESPONSE_TYPE)) {
            throw new InvalidRequestException(missingParameter(RESPONSE_TYPE));
        }
        String responseType = parameters.get(RESPONSE_TYPE);
        if (responseType == null || responseType.isEmpty()) {
            throw new ValidationFailedException("Invalid response type", "response_type must not be empty");
        }
        if (!"code".equals(responseType)) {
            throw new UnsupportedResponseTypeException("Unsupported response type: " + parameters.get(RESPONSE_TYPE));
        }
    }


}
