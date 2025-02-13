/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.in;

import com.fasterxml.jackson.databind.JsonNode;
import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.FlowController;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.exception.UnsupportedGrantTypeException;
import de.bdr.pidi.authorization.core.exception.ValidationFailedException;
import de.bdr.pidi.base.requests.SeedCredentialRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import static de.bdr.pidi.authorization.core.exception.InvalidRequestException.missingParameter;

class AuthenticationDelegate {
    private static final String GRANT_TYPE = "grant_type";

    private final HttpLibraryAdapter httpAdapter;
    private final FlowController flowController;

    AuthenticationDelegate(HttpLibraryAdapter httpAdapter, FlowController flowController) {
        this.httpAdapter = httpAdapter;
        this.flowController = flowController;
    }

    ResponseEntity<JsonNode> handlePar(MultiValueMap<String, String> allHeaders, MultiValueMap<String, String> allParams) {
        HttpRequest<?> request = httpAdapter.getLibraryHttpRequest(HttpMethod.POST, allHeaders, allParams);

        WResponseBuilder response = flowController.processPushedAuthRequest(request);

        return response.buildJSONResponseEntity();
    }

    ResponseEntity<String> handleAuthorize(MultiValueMap<String, String> allHeaders, MultiValueMap<String, String> allParams) {
        HttpRequest<?> request = httpAdapter.getLibraryHttpRequest(HttpMethod.GET, allHeaders, allParams);

        WResponseBuilder response = flowController.processAuthRequest(request);

        return response.buildStringResponseEntity();
    }

    ResponseEntity<String> handleFinishAuthorization(MultiValueMap<String, String> allHeaders, MultiValueMap<String, String> allParams) {
        var request = httpAdapter.getLibraryHttpRequest(HttpMethod.GET, allHeaders, allParams);

        WResponseBuilder response = flowController.processFinishAuthRequest(request);

        return response.buildStringResponseEntity();
    }

    ResponseEntity<JsonNode> handleInvalidGrantType(MultiValueMap<String, String> allParams) {
        var grantTypes = allParams.get(GRANT_TYPE);
        if (grantTypes == null || grantTypes.isEmpty()) {
            throw new ValidationFailedException(missingParameter(GRANT_TYPE));
        }
        var grantType = grantTypes.getFirst();
        if (grantType == null || grantType.isBlank()) {
            throw new ValidationFailedException(missingParameter(GRANT_TYPE));
        } else {
            throw new UnsupportedGrantTypeException("Grant type \"%s\" unsupported".formatted(grantType));
        }
    }

    ResponseEntity<JsonNode> handleToken(MultiValueMap<String, String> allHeaders, MultiValueMap<String, String> allParams) {
        HttpRequest<?> request = httpAdapter.getLibraryHttpRequest(HttpMethod.POST, allHeaders, allParams);

        WResponseBuilder response = flowController.processTokenRequest(request);

        return response.buildJSONResponseEntity();
    }

    ResponseEntity<JsonNode> handleRefreshToken(MultiValueMap<String, String> allHeaders, MultiValueMap<String, String> allParams) {
        HttpRequest<?> request = httpAdapter.getLibraryHttpRequest(HttpMethod.POST, allHeaders, allParams);

        WResponseBuilder response = flowController.processRefreshTokenRequest(request);

        return response.buildJSONResponseEntity();
    }

    ResponseEntity<JsonNode> handleSeedCredentialToken(MultiValueMap<String, String> allHeaders, MultiValueMap<String, String> allParams) {
        HttpRequest<?> request = httpAdapter.getLibraryHttpRequest(HttpMethod.POST, allHeaders, allParams);

        WResponseBuilder response = flowController.processSeedCredentialTokenRequest(request);

        return response.buildJSONResponseEntity();
    }

    ResponseEntity<JsonNode> handleCredential(FlowVariant flowVariant, MultiValueMap<String, String> allHeaders, MultiValueMap<String, String> allParams, String body) {
        HttpRequest<CredentialRequest> request = httpAdapter.getLibraryCredentialRequest(flowVariant, HttpMethod.POST, allHeaders, allParams, body);

        WResponseBuilder response = flowController.processCredentialRequest(request);

        return response.buildJSONResponseEntity();
    }

    @SuppressWarnings("unchecked") // casting the HttpRequest is safe, since the generic type has been checked
    ResponseEntity<JsonNode> handleSeedCredential(FlowVariant flowVariant, MultiValueMap<String, String> allHeaders, MultiValueMap<String, String> allParams, String body) {
        httpAdapter.validateCredentialRequest(body);

        HttpRequest<CredentialRequest> request = httpAdapter.getLibraryCredentialRequest(flowVariant, HttpMethod.POST, allHeaders, allParams, body);
        WResponseBuilder response;
        if (request.getBody() instanceof SeedCredentialRequest) {
            response = flowController.processSeedCredentialRequest((HttpRequest<SeedCredentialRequest>) (HttpRequest<?>) request);
        } else {
            response = flowController.processCredentialRequest(request);
        }
        return response.buildJSONResponseEntity();
    }

    ResponseEntity<JsonNode> handlePresentationSigning(MultiValueMap<String, String> allHeaders, MultiValueMap<String, String> allParams, String body) {
        var request = httpAdapter.getPresentationSigningRequest(HttpMethod.POST, allHeaders, allParams, body);

        WResponseBuilder response = flowController.processPresentationSigningRequest(request);

        return response.buildJSONResponseEntity();
    }
}
