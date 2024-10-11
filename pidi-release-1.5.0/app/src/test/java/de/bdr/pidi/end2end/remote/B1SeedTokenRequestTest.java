/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import de.bdr.openid4vc.vci.data.TokenType;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.SeedCredentialTokenRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.Pin;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

class B1SeedTokenRequestTest extends RemoteTest {
    private final Steps steps = new Steps(FlowVariant.B1);

    @DisplayName("Seed token endpoint happy path, variant b1")
    @Test
    @Requirement({"PIDI-851", "PIDI-1507", "PIDI-1510"})
    @XrayTest(key = "PIDI-1392")
    void test001() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .doRequest()
                .then().log().ifError()
                .assertThat()
                .status(HttpStatus.OK)
                .body("token_type", is(TokenType.DPOP.getValue()))
                .body("access_token", is(notNullValue()))
                .body("c_nonce", is(notNullValue()))
                .body("c_nonce", matchesPattern(TestUtils.NONCE_REGEX))
                .body("c_nonce_expires_in", isA(Integer.class))
                .body("expires_in", isA(Integer.class));
    }

    @DisplayName("Seed token endpoint invalid grant type, variant b1")
    @Test
    @Requirement({"PIDI-851", "PIDI-1510"})
    @XrayTest(key = "PIDI-1733")
    void test002() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withGrantType("foo")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_grant_type"))
                .body("error_description", is("Grant type \"foo\" unsupported"));
    }

    @DisplayName("Seed token endpoint empty grant type, variant b1")
    @Test
    @Requirement({"PIDI-1510"})
    @XrayTest(key = "PIDI-1764")
    void test020() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withGrantType("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'grant_type'"));
    }

    @DisplayName("Seed token endpoint missing grant type, variant b1")
    @Test
    @Requirement({"PIDI-851", "PIDI-1510"})
    @XrayTest(key = "PIDI-1735")
    void test003() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withoutGrantType()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'grant_type'"));
    }

    @DisplayName("Seed token endpoint invalid content type, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1746")
    void test004() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withContentType("text/javascript; charset=utf-8")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("Bad request"));
    }

    @DisplayName("Seed token endpoint empty seed credential, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1748")
    void test005() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withSeedCredential("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("seed_credential missing"));
    }

    @DisplayName("Seed token endpoint missing seed credential, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1742")
    void test006() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withoutSeedCredential()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("seed_credential missing"));
    }

    @DisplayName("Seed token endpoint invalid pin, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1744")
    void test007() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withSignedPop(Pin.createPin(null))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Session ID is missing"));
    }

    @DisplayName("Seed token endpoint missing pin signed nonce, variant b1")
    @Test
    @Requirement({"PIDI-851", "PIDI-1507"})
    @XrayTest(key = "PIDI-1737")
    void test008() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withoutPinDerivedEphKeyPop()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("pin_derived_eph_key_pop missing"));
    }

    @DisplayName("Seed token endpoint empty pin signed nonce, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1739")
    void test009() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withPinDerivedEphKeyPop("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("pin_derived_eph_key_pop missing"));
    }

    @DisplayName("Seed token endpoint missing device key signed nonce, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1740")
    void test010() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withoutDeviceKeyPop()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("device_key_pop missing"));
    }

    @DisplayName("Seed token endpoint empty device key signed nonce, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1743")
    void test011() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withDeviceKeyPop("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("device_key_pop missing"));
    }

    @DisplayName("Seed token endpoint invalid pop jwt, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1736")
    void test012() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        steps.doSessionRequest();

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, "dpopNonce", pin, credentialResponse.get("credential"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Session ID is unknown"));
    }

    @DisplayName("Seed token endpoint invalid device key signed nonce, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1738")
    void test013() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withDeviceKeyPop("invalid").doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("device_key_pop not a valid JWT"));
    }

    @DisplayName("Seed token endpoint invalid pin signed nonce, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1732")
    void test014() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withPinDerivedEphKeyPop("invalid").doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("pin_derived_eph_key_pop not a valid JWT"));
    }

    @DisplayName("Seed token endpoint invalid seed credential, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1734")
    void test015() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withSeedCredential("invalid").doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("Seed credential invalid"));
    }

    @DisplayName("Seed token endpoint pin still usable, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1745")
    void test016() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);
        var otherPin = Pin.createPin(TestUtils.C_NONCE);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .withSignedPop(otherPin)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Session ID is unknown"));
    }

    @DisplayName("Seed token endpoint last pin retry, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1747")
    void test017() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);

        var sessionId = steps.doSessionRequest();
        var differentPin = Pin.createPin(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, differentPin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, differentPin, credentialResponse.get("credential"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("PIN invalid"));

        var sessionId2 = steps.doSessionRequest();
        differentPin.updateNonce(sessionId2);
        dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, differentPin, null);
        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, differentPin, credentialResponse.get("credential"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("PIN invalid"));

        var sessionId3 = steps.doSessionRequest();
        differentPin.updateNonce(sessionId3);
        dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, differentPin, null);
        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, differentPin, credentialResponse.get("credential"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("PIN locked"));
    }

    @DisplayName("Seed token endpoint max pin retries exceeded, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1741")
    void test018() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);

        var sessionId = steps.doSessionRequest();
        var differentPin = Pin.createPin(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, differentPin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, differentPin, credentialResponse.get("credential"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("PIN invalid"));
        dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, differentPin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, differentPin, credentialResponse.get("credential"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("PIN invalid"));
        dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, differentPin, null);

        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, differentPin, credentialResponse.get("credential"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("PIN locked"));
    }

    @DisplayName("Seed token endpoint used issuer nonce, variant b1")
    @Test
    @Requirement("PIDI-851")
    @XrayTest(key = "PIDI-1755")
    void test019() {
        // With the current implementation, it is not possible to test a nonce that has already been used, so this error case is only tested implicitly
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var credentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var dpopNonce = steps.doSeedTokenDpopNonceRequest(deviceKeyPair, credentialResponse.get("credential"), clientId, pin, null);
        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, dpopNonce, pin, credentialResponse.get("credential"))
                .doRequest();
        SeedCredentialTokenRequestBuilder.valid(FlowVariant.B1, deviceKeyPair, clientId, "dpopNonce", pin, credentialResponse.get("credential"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"));
    }
}
