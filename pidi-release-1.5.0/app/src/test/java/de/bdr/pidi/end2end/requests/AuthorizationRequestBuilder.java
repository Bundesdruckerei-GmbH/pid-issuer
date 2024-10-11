/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.requests;


import de.bdr.pidi.authorization.FlowVariant;
import org.springframework.http.HttpMethod;

public class AuthorizationRequestBuilder extends RequestBuilder<AuthorizationRequestBuilder> {


    public AuthorizationRequestBuilder() {
        super(HttpMethod.GET);

    }

    public AuthorizationRequestBuilder(String url) {
        this();
        withUrl(url);
    }

    public static AuthorizationRequestBuilder valid(FlowVariant flowVariant, String clientId, String requestUri) {
        return new AuthorizationRequestBuilder()
                .withUrl(getAuthorizationPath(flowVariant))
                .withClientId(clientId)
                .withRequestUri(requestUri);
    }

    public static String getAuthorizationPath(FlowVariant flowVariant) {
        return "/%s/authorize".formatted(flowVariant.urlPath);
    }

    public AuthorizationRequestBuilder withClientId(String clientId) {
        withQueryParam("client_id", clientId);
        return this;
    }
    public AuthorizationRequestBuilder withRequestUri(String requestUri) {
        withQueryParam("request_uri", requestUri);
        return this;
    }

    public AuthorizationRequestBuilder withCodeChallenge(String codeChallenge) {
        withQueryParam("code_challenge", codeChallenge);
        return this;
    }

    public AuthorizationRequestBuilder withCodeChallengeMethod(String codeChallengeMethod) {
        withQueryParam("code_challenge_method", codeChallengeMethod);
        return this;
    }
}
