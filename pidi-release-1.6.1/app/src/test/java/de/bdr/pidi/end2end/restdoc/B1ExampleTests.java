/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.restdoc;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.AuthorizationRequestBuilder;
import de.bdr.pidi.end2end.requests.CredentialRequestBuilder;
import de.bdr.pidi.end2end.requests.Documentation;
import de.bdr.pidi.end2end.requests.FinishAuthorizationRequestBuilder;
import de.bdr.pidi.end2end.requests.PushedAuthorizationRequestBuilder;
import de.bdr.pidi.end2end.requests.SeedCredentialTokenRequestBuilder;
import de.bdr.pidi.end2end.requests.SessionRequestBuilder;
import de.bdr.pidi.end2end.requests.TokenRequestBuilder;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.Pin;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;

@Tag("e2e")
class B1ExampleTests extends RestDocTest {
    private static final String LOCATION = "Location";

    @Override
    FlowVariant flowVariant() {
        return FlowVariant.B1;
    }

    @Test
    @DisplayName("PAR happy path, variant b1")
    void test003() {
        var clientId = ClientIds.validClientId().toString();
        PushedAuthorizationRequestBuilder.valid(flowVariant(), clientId)
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.CREATED)
                .body("expires_in", is(60))
                .body("request_uri", startsWith("urn:ietf:params:oauth:request_uri:"))
                .body("request_uri", hasLength(56));
    }

    @Test
    @DisplayName("Authorize happy path, variant b1")
    void test004() {
        var clientId = ClientIds.validClientId().toString();
        String requestUri = steps.doPAR(clientId);
        AuthorizationRequestBuilder.valid(flowVariant(), clientId, requestUri)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.SEE_OTHER)
                .header(LOCATION, containsString("?SAMLRequest="));
    }

    @Test
    @DisplayName("Finish authorization happy path, variant b1")
    void test005() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(clientId, requestUri);
        FinishAuthorizationRequestBuilder.valid(flowVariant(), issuerState)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.FOUND)
                .header(LOCATION, startsWith("https://secure.redirect.com?"))
                .header(LOCATION, containsString("code="));
    }

    @Test
    @DisplayName("Credential request happy path with seed PID, variant b1")
    void test006() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(clientId, requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"), null);
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        CredentialRequestBuilder.validSeedPid(flowVariant(), deviceKeyPair, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"), pin)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credential", is(notNullValue()));
    }

    @Test
    @DisplayName("Credential request happy path with SdJwt, variant b1")
    void test007() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId, new Documentation("sdjwt/par"));
        var issuerState = steps.doAuthorize(clientId, requestUri, new Documentation("sdjwt/authorize"));
        var faResponse = steps.doFinishAuthorization(issuerState, new Documentation("sdjwt/finish-authorization"));
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"), new Documentation("sdjwt/token"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin, new Documentation("sdjwt/seed-credential"));
        var sessionId = steps.doSessionRequest(new Documentation("sdjwt/session"));
        pin.updateNonce(sessionId);
        var seedTokenDpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin, new Documentation("mdoc/seed-token-init"));
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin, seedTokenDpopNonce, new Documentation("mdoc/seed-token"));
        CredentialRequestBuilder
                .validSdJwt(flowVariant(), deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(flowVariant(), clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .withDocumentation(steps.prefixWithVariant(new Documentation("sdjwt/credential")))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credential", is(notNullValue()));
    }

    @Test
    @DisplayName("Credential request happy path with mdoc, variant b1")
    void test008() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId, new Documentation("mdoc/par"));
        var issuerState = steps.doAuthorize(clientId, requestUri, new Documentation("mdoc/authorize"));
        var faResponse = steps.doFinishAuthorization(issuerState, new Documentation("mdoc/finish-authorization"));
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"), new Documentation("mdoc/token"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin, new Documentation("mdoc/seed-credential"));
        var sessionId = steps.doSessionRequest(new Documentation("mdoc/session"));
        pin.updateNonce(sessionId);
        var seedTokenDpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin, new Documentation("mdoc/seed-token-init"));
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin, seedTokenDpopNonce, new Documentation("mdoc/seed-token"));
        CredentialRequestBuilder
                .validMdoc(flowVariant(), deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withDocumentation(steps.prefixWithVariant(new Documentation("mdoc/credential")))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credential", is(notNullValue()));
    }

    @Test
    @DisplayName("Token request, invalid grant-type = 'refresh_token', variant b1")
    void test009() {
        TokenRequestBuilder.valid(flowVariant(), "dpopNonce")
                .withGrantType("refresh_token")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_grant_type"))
                .body("error_description", is("Grant type \"refresh_token\" unsupported"));
    }

    @Test
    @DisplayName("Token request, empty grant-type, variant b1")
    void test010() {
        TokenRequestBuilder.valid(flowVariant(), "dpopNonce")
                .withGrantType("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'grant_type'"));
    }

    @Test
    @DisplayName("Token request, missing grant-type, variant b1")
    void test011() {
        TokenRequestBuilder.valid(flowVariant(), "dpopNonce")
                .withoutGrantType()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'grant_type'"));
    }

    @Test
    @DisplayName("Seed Credential Token request happy path, variant b1")
    void test012() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(clientId, requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"), null);
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin, null);
        var sessionId = steps.doSessionRequest(null);
        pin.updateNonce(sessionId);

        SeedCredentialTokenRequestBuilder.valid(flowVariant(), deviceKeyPair, clientId, "no_dpop_nonce_known", pin, seedCredentialResponse.get("credential"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("use_dpop_nonce"));
    }

    @Test
    @DisplayName("Seed Credential Token request, missing sessionId, variant b1")
    void test014() {
        var clientId = ClientIds.validClientId().toString();
        var pin = Pin.createPin("sessionId");
        SeedCredentialTokenRequestBuilder.valid(flowVariant(), TestUtils.DEVICE_KEY_PAIR, clientId, "dpopNonce", pin, "null")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Session ID is unknown"));
    }

    @Test
    @DisplayName("Session id request, happy path, variant b1")
    void test015() {
        SessionRequestBuilder.valid()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("session_id", is(notNullValue()))
                .body("session_id", matchesPattern(TestUtils.NONCE_REGEX))
                .body("session_id_expires_in", isA(Integer.class))
                .body("session_id_expires_in", greaterThan(0));
    }
}
