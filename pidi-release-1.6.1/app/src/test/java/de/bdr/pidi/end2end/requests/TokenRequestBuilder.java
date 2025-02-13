/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.requests;


import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.oauth2.sdk.GrantType;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.testdata.TestConfig;
import de.bdr.pidi.testdata.TestUtils;
import org.springframework.http.HttpMethod;

import java.net.URI;

import static de.bdr.pidi.testdata.ValidTestData.CODE_VERIFIER;
import static de.bdr.pidi.testdata.ValidTestData.REDIRECT_URI;

public class TokenRequestBuilder extends RequestBuilder<TokenRequestBuilder> {

    public static TokenRequestBuilder valid(FlowVariant flowVariant, String dpopNonce) {
        return valid(flowVariant, TestUtils.DEVICE_KEY_PAIR, dpopNonce);
    }

    public static TokenRequestBuilder valid(FlowVariant flowVariant, ECKey deviceKeyPair, String dpopNonce) {
        var path = getTokenPath(flowVariant);
        return new TokenRequestBuilder(path)
                .withContentType("application/x-www-form-urlencoded; charset=utf-8")
                .withRedirectUri(REDIRECT_URI)
                .withGrantType(GrantType.AUTHORIZATION_CODE.getValue())
                .withCodeVerifier(CODE_VERIFIER)
                .withDpopHeader(flowVariant, deviceKeyPair, dpopNonce)
                ;
    }

    public static String getTokenPath(FlowVariant flowVariant) {
        return "/%s/token".formatted(flowVariant.urlPath);
    }

    public static URI getTokenUri(FlowVariant flowVariant) {
        return URI.create(TestConfig.pidiBaseUrl() + getTokenPath(flowVariant));
    }

    public TokenRequestBuilder() {
        super(HttpMethod.POST);
    }

    public TokenRequestBuilder(String url) {
        this();
        withUrl(url);
    }

    public TokenRequestBuilder withRedirectUri(String redirectUri) {
        withFormParam("redirect_uri", redirectUri);
        return this;
    }

    public TokenRequestBuilder withGrantType(String grantType) {
        withFormParam("grant_type", grantType);
        return this;
    }

    public TokenRequestBuilder withAuthorizationCode(String authorizationCode) {
        withFormParam("code", authorizationCode);
        return this;
    }

    public TokenRequestBuilder withCodeVerifier(String codeVerifier) {
        withFormParam("code_verifier", codeVerifier);
        return this;
    }

    public TokenRequestBuilder withDpopHeader(FlowVariant flowVariant, String dpopNonce) {
        return withDpopHeader(flowVariant, httpMethod, dpopNonce);
    }

    public TokenRequestBuilder withDpopHeader(FlowVariant flowVariant, HttpMethod httpMethod, String dpopNonce) {
        var dpopProof = TestUtils.getDpopProof(httpMethod, getTokenUri(flowVariant), dpopNonce).serialize();
        withHeader("dpop", dpopProof);
        return this;
    }

    public TokenRequestBuilder withDpopHeader(FlowVariant flowVariant, ECKey deviceKeyPair, String dpopNonce) {
        var dpopProof = TestUtils.getDpopProof(deviceKeyPair, httpMethod, getTokenUri(flowVariant), dpopNonce).serialize();
        withHeader("dpop", dpopProof);
        return this;
    }

    public String getDpopProof() {
        var dpopValues = getHeaders("dpop");
        if (!dpopValues.isEmpty()) {
            return dpopValues.iterator().next();
        }
        return null;
    }

    public TokenRequestBuilder withoutCodeVerifier() {
        withoutFormParam("code_verifier");
        return this;
    }

    public TokenRequestBuilder withoutGrantType() {
        withoutFormParam("grant_type");
        return this;
    }

    public TokenRequestBuilder withoutRedirectUri() {
        withoutFormParam("redirect_uri");
        return this;
    }

    public TokenRequestBuilder withoutAuthorizationCode() {
        withoutFormParam("code");
        return this;
    }

    public TokenRequestBuilder withoutContentType() {
        withoutHeader("Content-Type");
        return this;
    }

    public TokenRequestBuilder withoutDpopHeader() {
        withoutHeader("dpop");
        return this;
    }
}
