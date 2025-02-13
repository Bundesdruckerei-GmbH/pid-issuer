/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.CredentialRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.Pin;
import de.bdr.pidi.testdata.TestUtils;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static de.bdr.pidi.end2end.requests.RequestBuilder.objectMapper;
import static de.bdr.pidi.testdata.TestUtils.CBOR_MAPPER;
import static de.bdr.pidi.testdata.TestUtils.ID_REGEX;
import static de.bdr.pidi.testdata.TestUtils.OBJECT_MAPPER;
import static de.bdr.pidi.testdata.TestUtils.getDisclosures;
import static de.bdr.pidi.testdata.TestUtils.toJsonNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class B1CredentialTest extends RemoteTest {
    private static final FlowVariant FLOW_VARIANT = FlowVariant.B1;
    private final Steps steps = new Steps(FLOW_VARIANT);

    @DisplayName("Credential endpoint happy path, variant b1")
    @Test
    @Requirement({"PIDI-370", "PIDI-282", "PIDI-241", "PIDI-234", "PIDI-828", "PIDI-1513", "PIDI-265"})
    @XrayTest(key = "PIDI-1256")
    void test001() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credential", is(notNullValue()))
                .body("c_nonce", is(notNullValue()))
                .body("c_nonce", matchesPattern(TestUtils.NONCE_REGEX))
                .body("c_nonce_expires_in", isA(Integer.class))
        ;
    }

    @DisplayName("Credential endpoint credential format unsupported, variant b1")
    @Test
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-1266")
    void test002() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder.validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withFormatAndVct("invalid", null)
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_credential_format"))
                .body("error_description", is("Credential format \"invalid\" not supported"));
    }

    @DisplayName("Credential endpoint credential type unsupported, variant b1")
    @Test
    @Requirement({"PIDI-370", "PIDI-316"})
    @XrayTest(key = "PIDI-1235")
    void test003() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withFormatAndVct("vc+sd-jwt", "invalid")
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_credential_type"))
                .body("error_description", is("Credential type \"invalid\" not supported"));
    }

    @DisplayName("Credential endpoint empty access token, variant b1")
    @Test
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-1245")
    void test004() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), "")
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("Credential endpoint invalid access token, variant b1")
    @Test
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-1298")
    void test005() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), "foo")
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", containsString("error=\"invalid_token\""));
    }

    @DisplayName("Credential endpoint invalid proof, variant b1")
    @Test
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-1309")
    void test006() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), "invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT credential nonce invalid"));
    }

    @DisplayName("Credential endpoint invalid proof issuer, variant b1")
    @Test
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-1276")
    void test007() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, "Foo", TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then().log().ifError()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT issuer invalid"));
    }

    @DisplayName("Credential endpoint invalid proof audience, variant b1")
    @Test
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-1288")
    @SuppressWarnings("java:S5976") // each test requires its own Requirement and XrayTest annotations
    void test008() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, "invalid", Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT audience invalid"));
    }

    @DisplayName("Credential endpoint invalid proof variant, variant b1")
    @Test
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-1191")
    void test009() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.C1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then().log().ifError()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT audience invalid"));
    }

    @DisplayName("Credential endpoint empty proof issuer, variant b1")
    @Test
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-1203")
    void test010() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, "", TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then().log().ifError()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT issuer invalid"));
    }

    @DisplayName("Credential endpoint empty proof audience, variant b1")
    @Test
    @Requirement({"PIDI-370", "PIDI-234"})
    @XrayTest(key = "PIDI-1209")
    void test011() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, "", Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT audience invalid"));
    }

    @DisplayName("Credential endpoint sd-jwt using neither proof nor proofs, variant b1")
    @Test
    @Requirement({"PIDI-234", "PIDI-708"})
    @XrayTest(key = "PIDI-1250")
    void test012() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder.validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof is missing"));
    }

    @DisplayName("Credential endpoint missing proof type, variant b1")
    @Test
    @Requirement("PIDI-234")
    @XrayTest(key = "PIDI-1261")
    void test013() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder.validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, Instant.now(), seedTokenResponse.get("c_nonce"), "")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT type mismatch, expected to be openid4vci-proof+jwt"));
    }

    @DisplayName("Credential endpoint invalid proof type, variant b1")
    @Test
    @Requirement("PIDI-234")
    @XrayTest(key = "PIDI-1231")
    void test014() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder.validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, Instant.now(), seedTokenResponse.get("c_nonce"), "fooo")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT type mismatch, expected to be openid4vci-proof+jwt"));
    }

    @DisplayName("Credential endpoint missing proof audience, variant b1")
    @Test
    @Requirement("PIDI-234")
    @XrayTest(key = "PIDI-1241")
    void test015() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder.validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, null, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT audience invalid"));
    }

    @DisplayName("Credential endpoint proof iat in future, variant b1")
    @Test
    @Requirement("PIDI-234")
    @XrayTest(key = "PIDI-1304")
    void test017() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder.validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now().plus(1, ChronoUnit.DAYS), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT is issued in the future"));
    }

    @DisplayName("Credential endpoint proof iat too old, variant b1")
    @Test
    @Requirement("PIDI-234")
    @XrayTest(key = "PIDI-1270")
    void test018() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder.validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now().minus(1, ChronoUnit.DAYS), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT issuance is too old"));
    }

    @DisplayName("Credential endpoint proof invalid jwt signature, variant b1")
    @Test
    @Requirement("PIDI-234")
    @XrayTest(key = "PIDI-1281")
    void test019() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder.validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withInvalidProof(FlowVariant.B1, clientId, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT signature is invalid"));
    }

    @DisplayName("Credential endpoint invalid dpop, variant b1")
    @Test
    @Requirement({"PIDI-246", "PIDI-227"})
    @XrayTest(key = "PIDI-1219")
    void test020() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, "foo", seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", containsString("error=\"use_dpop_nonce\""))
                .header("WWW-Authenticate", containsString("error_description=\"DPoP nonce is invalid\""))
                .header("WWW-Authenticate", not(containsString("DPoP-Nonce:")))
                .header("DPoP-Nonce", is(notNullValue()));
    }

    @DisplayName("Credential request not parseable jwt, variant b1")
    @Test
    @Requirement("PIDI-234")
    @XrayTest(key = "PIDI-1224")
    void test021() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withInvalidProof()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT could not be parsed"));
    }

    @DisplayName("Credential endpoint request without dpop, variant b1")
    @Test
    @Requirement("PIDI-745")
    @XrayTest(key = "PIDI-1207")
    void test022() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("c_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .withDpopHeader(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("access_token"), null)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", containsString("error=\"use_dpop_nonce\""))
                .header("WWW-Authenticate", containsString("error_description=\"nonce value missing\""))
                .header("WWW-Authenticate", not(containsString("DPoP-Nonce:")))
                .header("DPoP-Nonce", notNullValue());

    }


    @DisplayName("Credential request happy path with mdoc, variant b1")
    @Test
    @Requirement("PIDI-729")
    @XrayTest(key = "PIDI-1340")
    void test023() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validMdoc(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .doRequest()
                .then().log().ifError()
                .assertThat()
                .status(HttpStatus.OK)
                .header("Content-Type", is("application/json"))
                .body("credential", is(notNullValue()))
                .body("c_nonce", is(notNullValue()))
                .body("c_nonce", matchesPattern(TestUtils.NONCE_REGEX))
                .body("c_nonce_expires_in", isA(Integer.class));
    }

    @DisplayName("Credential endpoint credential format unsupported with mdoc, variant b1")
    @Test
    @Requirement("PIDI-729")
    @XrayTest(key = "PIDI-1341")
    void test024() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder.validMdoc(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withFormatAndVct("invalid", null)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_credential_format"))
                .body("error_description", is("Credential format \"invalid\" not supported"));
    }

    @DisplayName("Credential endpoint credential type unsupported with mdoc, variant b1")
    @Test
    @Requirement("PIDI-729")
    @XrayTest(key = "PIDI-1342")
    void test025() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validMdoc(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withFormatAndVct("vc+sd-jwt", "invalid")
                .doRequest()
                .then().log().ifError()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_credential_type"))
                .body("error_description", is("Credential type \"invalid\" not supported"));
    }

    @DisplayName("Credential endpoint empty access token with mdoc, variant b1")
    @Test
    @Requirement("PIDI-729")
    @XrayTest(key = "PIDI-1343")
    void test026() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder.validMdoc(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), "")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("Credential endpoint invalid access token with mdoc, variant b1")
    @Test
    @Requirement("PIDI-729")
    @XrayTest(key = "PIDI-1344")
    void test027() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder.validMdoc(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), "foo")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", containsString("error=\"invalid_token\""));
    }


    @DisplayName("Credential endpoint invalid dpop with mdoc, variant b1")
    @Test
    @Requirement("PIDI-729")
    @XrayTest(key = "PIDI-1345")
    void test028() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder.validMdoc(FlowVariant.B1, deviceKeyPair, "foo", seedTokenResponse.get("access_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", containsString("error=\"use_dpop_nonce\""))
                .header("WWW-Authenticate", containsString("error_description=\"DPoP nonce is invalid\""))
                .header("WWW-Authenticate", not(containsString("DPoP-Nonce:")))
                .header("DPoP-Nonce", is(notNullValue()));
    }

    @DisplayName("Credential endpoint request without dpop with mdoc, variant b1")
    @Test
    @Requirement("PIDI-729")
    @XrayTest(key = "PIDI-1346")
    void test029() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validMdoc(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withDpopHeader(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("access_token"), null)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", containsString("error=\"use_dpop_nonce\""))
                .header("WWW-Authenticate", containsString("error_description=\"nonce value missing\""))
                .header("WWW-Authenticate", not(containsString("DPoP-Nonce:")))
                .header("DPoP-Nonce", notNullValue());

    }

    @DisplayName("Credential request sd-jwt with proofs failed, variant b1")
    @Test
    @Requirement("PIDI-1149")
    @XrayTest(key = "PIDI-1403")
    void test030() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProofs(FlowVariant.B1, clientId, Instant.now(), seedTokenResponse.get("c_nonce"), 2)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("No proofs expected"));
    }

    @DisplayName("Credential request mdoc with proof failed, variant b1")
    @Test
    @Requirement("PIDI-1149")
    @XrayTest(key = "PIDI-1407")
    void test031() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);
        CredentialRequestBuilder
                .validMdoc(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("Neither proof nor proofs expected"));
    }

    @DisplayName("Credential request mdoc with proofs failed, variant b1")
    @Test
    @Requirement("PIDI-1149")
    @XrayTest(key = "PIDI-1408")
    void test032() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);
        CredentialRequestBuilder
                .validMdoc(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProofs(FlowVariant.B1, clientId, Instant.now(), seedTokenResponse.get("c_nonce"), 2)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("Neither proof nor proofs expected"));
    }

    @DisplayName("Credential request sd-jwt missing format, variant b1")
    @Test
    @Requirement("PIDI-828")
    @XrayTest(key = "PIDI-1493")
    void test033() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);
        var body = objectMapper.createObjectNode()
                .put("format", "")
                .put("vct", TestUtils.SD_JWT_VCTYPE);
        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("format parameter is missing"));
    }

    @DisplayName("Credential request sd-jwt unsupported format, variant b1")
    @Test
    @Requirement("PIDI-828")
    @XrayTest(key = "PIDI-1496")
    void test034() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);
        var body = objectMapper.createObjectNode()
                .put("format", "unsupported_format")
                .put("vct", TestUtils.SD_JWT_VCTYPE);
        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token")).withJsonBody(body)
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_credential_format"))
                .body("error_description", is("Credential format \"unsupported_format\" not supported"));
    }

    @DisplayName("Credential endpoint missing verifier pub, variant b1")
    @Test
    @Requirement({"PIDI-370", "PIDI-282", "PIDI-241", "PIDI-234", "PIDI-828", "PIDI-1513"})
    @XrayTest(key = "PIDI-2142")
    void test035() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .withRemovedJsonBodyProperty("verifier_pub")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"));
    }

    @DisplayName("Credential endpoint sd-jwt using proof and proofs, variant b1")
    @Test
    @Requirement({"PIDI-708"})
    @XrayTest(key = "PIDI-2233")
    void test036() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .withProofs(FlowVariant.B1, clientId, Instant.now(), seedTokenResponse.get("c_nonce"), 2)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("Only proof OR proofs can be set, not both"));
    }

    @DisplayName("Credential endpoint sd-jwt empty jwt list, variant b1")
    @Test
    @Requirement({"PIDI-708"})
    @XrayTest(key = "PIDI-2232")
    void test038() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);
        var body = objectMapper.createObjectNode();
        body.putObject("proofs").putArray("jwt");
        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProofs(FlowVariant.B1, clientId, Instant.now(), seedTokenResponse.get("c_nonce"), 2)
                .withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof is missing"))
        ;
    }

    @DisplayName("Credential endpoint sd-jwt missing jwt list, variant b1")
    @Test
    @Requirement({"PIDI-708"})
    @XrayTest(key = "PIDI-2235")
    void test039() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);
        var body = objectMapper.createObjectNode();
        body.putObject("proofs");
        CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProofs(FlowVariant.B1, clientId, Instant.now(), seedTokenResponse.get("c_nonce"), 2)
                .withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT could not be parsed"))
        ;
    }

    @DisplayName("Credential endpoint mdoc hmaced format, variant b1")
    @Test
    @Requirement({"PIDI-242"})
    @XrayTest(key = "PIDI-2384")
    void test040() throws IOException {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        var credentialResponse = CredentialRequestBuilder
                .validMdoc(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .doRequest()
                .then();
        var jsonPathObj = credentialResponse.extract().body().jsonPath();
        String credential = jsonPathObj.getString("credential");
        byte[] decodedMDoc = java.util.Base64.getUrlDecoder().decode(credential);
        Map<String, Object> mdocMap = CBOR_MAPPER.readValue(decodedMDoc, new TypeReference<>() {
        });
        assertThat(mdocMap).hasSize(3).containsKey("docType").containsKey("issuerSigned").containsKey("deviceSigned");
        Assertions.assertEquals("eu.europa.ec.eudi.pid.1", CBOR_MAPPER.readTree(decodedMDoc).findValue("docType").asText());
        Map<String, Object> deviceSigned = CBOR_MAPPER.convertValue(mdocMap.get("deviceSigned"), new TypeReference<>() {
        });
        assertThat(deviceSigned).hasSize(2).containsKey("nameSpaces").containsKey("deviceAuth");
        Map<String, Object> deviceAuth = CBOR_MAPPER.convertValue(deviceSigned.get("deviceAuth"), new TypeReference<>() {
        });
        assertThat(deviceAuth).containsKey("deviceMac").hasSize(1);
        Map<String, Object> issuerSigned = CBOR_MAPPER.convertValue(mdocMap.get("issuerSigned"), new TypeReference<>() {
        });
        assertThat(issuerSigned).hasSizeGreaterThanOrEqualTo(1).containsKey("issuerAuth");
    }

    @DisplayName("Credential endpoint sd jwt contains x5c header, variant b1")
    @Test
    @XrayTest(key = "PIDI-2548")
    void test041() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        var body = CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest().body();
        var jsonPath = body.jsonPath();
        String[] splitString = jsonPath.getString("credential").split("\\.");
        String base64EncodedBody = splitString[0];
        String jsonString = new String(Base64.decodeBase64(base64EncodedBody), StandardCharsets.UTF_8);
        StringReader reader = new StringReader(jsonString);
        JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
        assertThat(jsonObject.get("x5c").getAsJsonArray()).isNotEmpty();
        jsonObject.get("x5c").getAsJsonArray();
        String x5cHeader = jsonObject.get("x5c").getAsJsonArray().get(0).getAsString();
        assertNotNull(x5cHeader);
    }

    @DisplayName("Credential request all claims check, variant b1")
    @Test
    @Requirement({"PIDI-316","PIDI-2202"})
    @XrayTest(key = "PIDI-2595")
    @SuppressWarnings("java:S5961") // checking the credential requires more than 25 assertions
    void test042() {
        java.util.Base64.Decoder urlDecoder = java.util.Base64.getUrlDecoder();
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var seedCredentialResponse = steps.doSeedCredentialRequest(deviceKeyPair, tokenResponse.get("access_token"), tokenResponse.get("dpop_nonce"), pin);
        var sessionId = steps.doSessionRequest();
        pin.updateNonce(sessionId);
        var seedTokenResponse = steps.doSeedTokenRequest(deviceKeyPair, seedCredentialResponse.get("credential"), clientId, pin);

        var body = CredentialRequestBuilder
                .validSdJwt(FlowVariant.B1, deviceKeyPair, seedTokenResponse.get("dpop_nonce"), seedTokenResponse.get("access_token"))
                .withProof(FlowVariant.B1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), seedTokenResponse.get("c_nonce"))
                .doRequest().body();
        var jsonPath = body.jsonPath();
        String[] splitString = jsonPath.getString("credential").split("\\.");
        String base64EncodedBody = splitString[1];
        String base64EncodedSignature = splitString[2];
        List<String> disclosures = getDisclosures(base64EncodedSignature);
        HashMap<String, JsonNode> parsedDisclosuresMap = disclosures.stream()
                .map(disclosureJson -> new String(urlDecoder.decode(disclosureJson)))
                .map(toJsonNode())
                .collect(Collectors.toMap(disclosureJsonNode -> disclosureJsonNode.get(1).asText(),
                        disclosureJsonNode -> disclosureJsonNode.get(2),
                        (a, b) -> new ArrayNode(OBJECT_MAPPER.getNodeFactory()).add(a).add(b),
                        HashMap::new));
        String jsonString = new String(Base64.decodeBase64(base64EncodedBody), StandardCharsets.UTF_8);
        StringReader reader = new StringReader(jsonString);
        JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
        assertAll(
                () -> assertEquals("MUSTERMANN", parsedDisclosuresMap.get("family_name").asText()),
                () -> assertEquals("ERIKA", parsedDisclosuresMap.get("given_name").asText()),
                () -> assertEquals("1964-08-12", parsedDisclosuresMap.get("birthdate").asText()),
                () -> assertEquals("1964", parsedDisclosuresMap.get("age_birth_year").asText()),
                () -> assertTrue(parsedDisclosuresMap.get("12").asBoolean()),
                () -> assertTrue(parsedDisclosuresMap.get("14").asBoolean()),
                () -> assertTrue(parsedDisclosuresMap.get("16").asBoolean()),
                () -> assertTrue(parsedDisclosuresMap.get("18").asBoolean()),
                () -> assertTrue(parsedDisclosuresMap.get("21").asBoolean()),
                () -> assertFalse(parsedDisclosuresMap.get("65").asBoolean()),
                () -> assertEquals(2, parsedDisclosuresMap.get("locality").size()),
                () -> assertEquals("BERLIN", parsedDisclosuresMap.get("locality").get(0).asText()),
                () -> assertEquals("KÖLN", parsedDisclosuresMap.get("locality").get(1).asText()),
                () -> assertEquals("DE", parsedDisclosuresMap.get("country").asText()),
                () -> assertEquals("51147", parsedDisclosuresMap.get("postal_code").asText()),
                () -> assertEquals("HEIDESTRAẞE 17", parsedDisclosuresMap.get("street_address").asText()),
                () -> assertNotNull(jsonObject.get("issuing_authority")),
                () -> assertEquals("DE", jsonObject.get("issuing_authority").isJsonNull() ? "" : jsonObject.get("issuing_authority").getAsString()),
                () -> assertNotNull(jsonObject.get("issuing_country")),
                () -> assertEquals("DE", jsonObject.get("issuing_country").getAsString()),
                () -> assertNotNull(jsonObject.get("iat")),
                () -> assertThat(Instant.ofEpochSecond(jsonObject.get("iat").getAsLong())).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS)),
                () -> assertNotNull(jsonObject.get("exp")),
                () -> assertThat(Instant.ofEpochSecond(jsonObject.get("exp").getAsLong())).isCloseTo(Instant.now(), within(14, ChronoUnit.DAYS)),
                () -> assertThat(jsonObject.get("iss").getAsString()).isNotNull().matches("http.*/" + FLOW_VARIANT.urlPath),
                () -> assertNotNull(jsonObject.getAsJsonObject("status")),
                () -> assertNotNull(jsonObject.getAsJsonObject("status").getAsJsonObject("status_list"))
        );
        var statusListJsonObject = jsonObject.getAsJsonObject("status").getAsJsonObject("status_list");
        String statusUri = statusListJsonObject.get("uri").getAsString();
        assertAll(
                () -> assertThat(statusListJsonObject.get("idx").getAsInt()).isNotNegative(),
                () -> assertThat(statusListJsonObject.get("uri").getAsString()).isNotNull().matches("http.*"),
                () -> assertThat(statusUri.substring(statusUri.lastIndexOf('/') + 1)).matches(ID_REGEX)
        );
    }
}
