/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.requests;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.testdata.Pin;
import de.bdr.pidi.testdata.TestUtils;
import org.springframework.http.HttpMethod;

public class SeedCredentialTokenRequestBuilder extends RequestBuilder<SeedCredentialTokenRequestBuilder> {

    public static SeedCredentialTokenRequestBuilder valid(FlowVariant flowVariant, ECKey deviceKeyPair, String clientId, String dpopNonce, Pin pin, String seedCredential) {
        var path = TokenRequestBuilder.getTokenPath(flowVariant);
        return new SeedCredentialTokenRequestBuilder(path)
                .withContentType("application/x-www-form-urlencoded; charset=utf-8")
                .withGrantType("urn:ietf:params:oauth:grant-type:seed_credential") // there is no such GrantType in nimbus-lib
                .withSeedCredential(seedCredential)
                .withSignedPop(pin)
                .withClientId(clientId)
                .withDpopHeader(flowVariant, deviceKeyPair, dpopNonce)
                ;
    }

    public SeedCredentialTokenRequestBuilder(String url) {
        this();
        withUrl(url);
    }

    public SeedCredentialTokenRequestBuilder() {
        super(HttpMethod.POST);
    }

    public SeedCredentialTokenRequestBuilder withGrantType(String grantType) {
        withFormParam("grant_type", grantType);
        return this;
    }

    public SeedCredentialTokenRequestBuilder withClientId(String clientId) {
        withFormParam("client_id", clientId);
        return this;
    }

    public SeedCredentialTokenRequestBuilder withDpopHeader(FlowVariant flowVariant, ECKey deviceKeyPair, String dpopNonce) {
        final SignedJWT dpopProof = TestUtils.getDpopProof(deviceKeyPair, httpMethod, TokenRequestBuilder.getTokenUri(flowVariant), dpopNonce);
        withHeader("dpop", dpopProof.serialize());
        return this;
    }

    public SeedCredentialTokenRequestBuilder withSeedCredential(String credential) {
        withFormParam("seed_credential", credential);
        return this;
    }

    public SeedCredentialTokenRequestBuilder withSignedPop(Pin pin) {
        withFormParam("pin_derived_eph_key_pop", pin.pinDerivedEphKeyPop().serialize());
        withFormParam("device_key_pop", pin.deviceKeyPop().serialize());
        return this;
    }

    public SeedCredentialTokenRequestBuilder withPinDerivedEphKeyPop(String pinDerivedEphKeyPop) {
        withFormParam("pin_derived_eph_key_pop", pinDerivedEphKeyPop);
        return this;
    }

    public SeedCredentialTokenRequestBuilder withDeviceKeyPop(String deviceKeyPop) {
        withFormParam("device_key_pop", deviceKeyPop);
        return this;
    }

    public SeedCredentialTokenRequestBuilder withoutGrantType() {
        withoutFormParam("grant_type");
        return this;
    }

    public SeedCredentialTokenRequestBuilder withoutSeedCredential() {
        withoutFormParam("seed_credential");
        return this;
    }

    public SeedCredentialTokenRequestBuilder withoutPinDerivedEphKeyPop() {
        withoutFormParam("pin_derived_eph_key_pop");
        return this;
    }

    public SeedCredentialTokenRequestBuilder withoutDeviceKeyPop() {
        withoutFormParam("device_key_pop");
        return this;
    }
}
