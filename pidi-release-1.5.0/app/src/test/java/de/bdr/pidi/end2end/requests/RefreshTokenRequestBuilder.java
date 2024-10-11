/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.requests;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.GrantType;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.testdata.TestUtils;
import org.springframework.http.HttpMethod;

public class RefreshTokenRequestBuilder extends RequestBuilder<RefreshTokenRequestBuilder> {
    public static RefreshTokenRequestBuilder valid(FlowVariant flowVariant, String clientId, String dpopNonce, String refreshToken) {
        var path = TokenRequestBuilder.getTokenPath(flowVariant);
        return new RefreshTokenRequestBuilder(path)
                .withContentType("application/x-www-form-urlencoded; charset=utf-8")
                .withGrantType(GrantType.REFRESH_TOKEN.getValue())
                .withClientId(clientId)
                .withDpopHeader(flowVariant, dpopNonce)
                .withClientAssertionType("urn:ietf:params:oauth:client-assertion-type:jwt-client-attestation")
                .withClientAssertion(TestUtils.getValidClientAssertion(flowVariant))
                .withRefreshToken(refreshToken);
    }

    public RefreshTokenRequestBuilder(String url) {
        this();
        withUrl(url);
    }

    public RefreshTokenRequestBuilder() {
        super(HttpMethod.POST);
    }

    public RefreshTokenRequestBuilder withGrantType(String grantType) {
        withFormParam("grant_type", grantType);
        return this;
    }

    public RefreshTokenRequestBuilder withRefreshToken(String refreshToken) {
        withFormParam("refresh_token", refreshToken);
        return this;
    }

    public RefreshTokenRequestBuilder withClientId(String clientId) {
        withFormParam("client_id", clientId);
        return this;
    }

    public RefreshTokenRequestBuilder withDpopHeader(FlowVariant flowVariant, String dpopNonce) {
        return withDpopHeader(flowVariant, TestUtils.DEVICE_KEY_PAIR, dpopNonce);
    }

    public RefreshTokenRequestBuilder withDpopHeader(FlowVariant flowVariant, ECKey deviceKeyPair, String dpopNonce) {
        final SignedJWT dpopProof = TestUtils.getDpopProof(deviceKeyPair, HttpMethod.POST, TokenRequestBuilder.getTokenUri(flowVariant), dpopNonce);
        withHeader("dpop", dpopProof.serialize());
        return this;
    }

    public RefreshTokenRequestBuilder withClientAssertion(String clientAssertion) {
        withFormParam("client_assertion", clientAssertion);
        return this;
    }

    public RefreshTokenRequestBuilder withClientAssertionType(String clientAssertionType) {
        withFormParam("client_assertion_type", clientAssertionType);
        return this;
    }

    public RefreshTokenRequestBuilder withOptionalScope(String scope) {
        withFormParam("scope", scope);
        return this;
    }

    public RefreshTokenRequestBuilder withoutGrantType() {
        withoutFormParam("grant_type");
        return this;
    }

    public RefreshTokenRequestBuilder withoutRefreshToken() {
        withoutFormParam("refresh_token");
        return this;
    }
}
