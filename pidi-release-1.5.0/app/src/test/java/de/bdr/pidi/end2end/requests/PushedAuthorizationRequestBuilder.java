/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.requests;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.testdata.ClientIds;
import org.springframework.http.HttpMethod;

import static de.bdr.pidi.testdata.ValidTestData.CODE_CHALLANGE;
import static de.bdr.pidi.testdata.ValidTestData.REDIRECT_URI;

public class PushedAuthorizationRequestBuilder extends RequestBuilder<PushedAuthorizationRequestBuilder> {

    public PushedAuthorizationRequestBuilder() {
        super(HttpMethod.POST);
    }

    public static PushedAuthorizationRequestBuilder valid(FlowVariant flowVariant, String clientId) {
        return valid(clientId)
                .withUrl(getPARPath(flowVariant));
    }

    public static PushedAuthorizationRequestBuilder valid(FlowVariant flowVariant) {
        return valid()
                .withUrl(getPARPath(flowVariant));
    }

    /**
     * Creates a valid PushedAuthorizationRequest by default.
     */
    public static PushedAuthorizationRequestBuilder valid() {
        return valid(ClientIds.validClientId().toString());
    }

    // TODO withUrl should be part of valid, instead of being called in each test
    public static PushedAuthorizationRequestBuilder valid(String clientId) {
        return new PushedAuthorizationRequestBuilder()
                .withCodeChallengeMethod("S256")
                .withCodeChallenge(CODE_CHALLANGE)
                .withResponseType("code")
                .withScope("pid")
                .withClientId(clientId)
                .withContentType("application/x-www-form-urlencoded")
                .withRedirectUri(REDIRECT_URI);
    }

    public static String getPARPath(FlowVariant flowVariant) {
        return "/%s/par".formatted(flowVariant.urlPath);
    }

    public PushedAuthorizationRequestBuilder withCodeChallenge(String codeChallenge) {
        withFormParam("code_challenge", codeChallenge);
        return this;
    }

    public PushedAuthorizationRequestBuilder withCodeChallengeMethod(String codeChallengeMethod) {
        withFormParam("code_challenge_method", codeChallengeMethod);
        return this;
    }

    public PushedAuthorizationRequestBuilder withClientId(String clientId) {
        withFormParam("client_id", clientId);
        return this;
    }

    public PushedAuthorizationRequestBuilder withRedirectUri(String redirectUri) {
        withFormParam("redirect_uri", redirectUri);
        return this;
    }

    public PushedAuthorizationRequestBuilder withState(String state) {
        withFormParam("state", state);
        return this;
    }

    public PushedAuthorizationRequestBuilder withResponseType(String responseType) {
        withFormParam("response_type", responseType);
        return this;
    }

    public PushedAuthorizationRequestBuilder withScope(String scope) {
        withFormParam("scope", scope);
        return this;
    }

    public PushedAuthorizationRequestBuilder withClientAssertion(String clientAssertion) {
        withFormParam("client_assertion", clientAssertion);
        return this;
    }

    public PushedAuthorizationRequestBuilder withClientAssertionType(String clientAssertionType) {
        withFormParam("client_assertion_type", clientAssertionType);
        return this;
    }

    public PushedAuthorizationRequestBuilder withoutClientId() {
        withoutFormParam("client_id");
        return this;
    }

    public PushedAuthorizationRequestBuilder withoutResponseType() {
        withoutFormParam("response_type");
        return this;
    }

    public PushedAuthorizationRequestBuilder withoutScope() {
        withoutFormParam("scope");
        return this;
    }

    public PushedAuthorizationRequestBuilder withoutRedirectUri() {
        withoutFormParam("redirect_uri");
        return this;
    }
    public PushedAuthorizationRequestBuilder withoutCodeChallenge() {
        withoutFormParam("code_challenge");
        return this;
    }
    public PushedAuthorizationRequestBuilder withoutCodeChallengeMethod() {
        withoutFormParam("code_challenge_method");
        return this;
    }
    public PushedAuthorizationRequestBuilder withoutClientAssertion() {
        withoutFormParam("client_assertion");
        return this;
    }
    public PushedAuthorizationRequestBuilder withoutClientAssertionType() {
        withoutFormParam("client_assertion_type");
        return this;
    }
}
