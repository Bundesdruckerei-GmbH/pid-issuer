/*
 * Copyright 2024-2025 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.CredentialRequestBuilder;
import de.bdr.pidi.end2end.requests.MetadataRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.TestUtils;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.bdr.pidi.end2end.requests.RequestBuilder.objectMapper;
import static de.bdr.pidi.testdata.TestUtils.ID_REGEX;
import static de.bdr.pidi.testdata.TestUtils.OBJECT_MAPPER;
import static de.bdr.pidi.testdata.TestUtils.getDisclosures;
import static de.bdr.pidi.testdata.TestUtils.readMdocIssuerAuth;
import static de.bdr.pidi.testdata.TestUtils.readPidValues;
import static de.bdr.pidi.testdata.TestUtils.toJsonNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
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

class C1CredentialTest extends RemoteTest {
    private static final FlowVariant FLOW_VARIANT = FlowVariant.C1;
    private final Steps steps = new Steps(FLOW_VARIANT);
    private static final String BASE_URL_REGEX = "https?://\\w++(?:\\.[\\w\\-]+)*+(?::\\d+)?";

    @Test
    @DisplayName("Credential request happy path, variant c1")
    @Requirement({"PIDI-370", "PIDI-282", "PIDI-241", "PIDI-265"})
    @XrayTest(key = "PIDI-650")
    void test001() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK).header("Content-Type", is("application/json"))
                .body("credential", is(notNullValue()))
                .body("c_nonce", is(notNullValue()))
                .body("c_nonce", matchesPattern(TestUtils.NONCE_REGEX))
                .body("c_nonce_expires_in", isA(Integer.class));
    }

    @Test
    @DisplayName("Credential endpoint, batch request happy path with SdJwt, variant c1")
    @Requirement("PIDI-672")
    @XrayTest(key = "PIDI-1074")
    void test028() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProofs(FLOW_VARIANT, clientId, Instant.now(), tokenResponse.get("c_nonce"), 2)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .header("Content-Type", is("application/json"))
                .body("credentials", hasSize(2))
                .body("c_nonce", is(notNullValue()))
                .body("c_nonce", matchesPattern(TestUtils.NONCE_REGEX))
                .body("c_nonce_expires_in", isA(Integer.class))
        ;
    }

    @Test
    @DisplayName("Credential endpoint, batch request happy path with mdoc, variant c1")
    @Requirement("PIDI-672")
    @XrayTest(key = "PIDI-1075")
    void test029() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validMdoc(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProofs(FLOW_VARIANT, clientId, Instant.now(), tokenResponse.get("c_nonce"), 2)
                .doRequest()
                .then()
                .assertThat()
                .header("Content-Type", is("application/json"))
                .body("credentials", hasSize(2))
                .body("c_nonce", is(notNullValue()))
                .body("c_nonce", matchesPattern(TestUtils.NONCE_REGEX))
                .body("c_nonce_expires_in", isA(Integer.class))
        ;
    }

    @DisplayName("Credential endpoint credential format unsupported, variant c1")
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-639")
    @Test
    void test002() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withFormatAndVct("invalid", null)
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_credential_format"))
                .body("error_description", is("Credential format \"invalid\" not supported"));
    }

    @DisplayName("Credential endpoint credential type unsupported, variant c1")
    @Requirement({"PIDI-370", "PIDI-316"})
    @XrayTest(key = "PIDI-645")
    @Test
    void test003() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withFormatAndVct("vc+sd-jwt", "invalid")
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_credential_type"))
                .body("error_description", is("Credential type \"invalid\" not supported"));
    }

    @DisplayName("Credential endpoint empty access token, variant c1")
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-646")
    @Test
    void test004() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), "")
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat().status(HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("Credential endpoint invalid access token, variant c1")
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-647")
    @Test
    void test005() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), "foo")
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", containsString("error=\"invalid_token\""));
    }

    @DisplayName("Credential endpoint invalid proof, variant c1")
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-649")
    @Test
    void test006() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), "invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT credential nonce invalid"));
    }

    @DisplayName("Credential endpoint invalid proof issuer, variant c1")
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-648")
    @Test
    void test007() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, "Foo", TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT issuer invalid"));
    }

    @DisplayName("Credential endpoint invalid proof audience, variant c1")
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-657")
    @Test
    @SuppressWarnings("java:S5976") // each test requires its own Requirement and XrayTest annotations
    void test008() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, "invalid", Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT audience invalid"));
    }

    @DisplayName("Credential endpoint invalid proof variant, variant c1")
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-660")
    @Test
    void test009() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FlowVariant.C, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT audience invalid"));
    }

    @DisplayName("Credential endpoint empty proof issuer, variant c1")
    @Requirement("PIDI-370")
    @XrayTest(key = "PIDI-661")
    @Test
    void test010() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, "", TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT issuer invalid"));
    }

    @DisplayName("Credential endpoint empty proof audience, variant c1")
    @Requirement({"PIDI-370", "PIDI-234"})
    @XrayTest(key = "PIDI-664")
    @Test
    void test011() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, "", Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT audience invalid"));
    }

    @Test
    @DisplayName("Credential request using neither proof nor proofs, variant c1")
    @Requirement({"PIDI-234", "PIDI-708"})
    @XrayTest(key = "PIDI-681")
    void test012() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder.validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof is missing"));
    }

    @Test
    @DisplayName("Credential endpoint missing proof type, variant c1")
    @Requirement("PIDI-234")
    @XrayTest(key = "PIDI-684")
    void test013() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder.validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, Instant.now(), tokenResponse.get("c_nonce"), "")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT type mismatch, expected to be openid4vci-proof+jwt"));
    }

    @Test
    @DisplayName("Credential endpoint invalid proof type, variant c1")
    @Requirement("PIDI-234")
    @XrayTest(key = "PIDI-685")
    void test014() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder.validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, Instant.now(), tokenResponse.get("c_nonce"), "foo")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT type mismatch, expected to be openid4vci-proof+jwt"));
    }

    @Test
    @DisplayName("Credential endpoint missing proof audience, variant c1")
    @Requirement("PIDI-234")
    @XrayTest(key = "PIDI-688")
    void test015() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder.validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, null, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT audience invalid"));
    }

    @Test
    @DisplayName("Credential endpoint proof iat in future, variant c1")
    @Requirement("PIDI-234")
    @XrayTest(key = "PIDI-695")
    void test017() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder.validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now().plus(1, ChronoUnit.DAYS), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT is issued in the future"));
    }

    @Test
    @DisplayName("Credential endpoint proof iat too old, variant c1")
    @Requirement("PIDI-234")
    @XrayTest(key = "PIDI-697")
    void test018() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder.validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now().minus(1, ChronoUnit.DAYS), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT issuance is too old"));
    }

    @Test
    @DisplayName("Credential endpoint proof invalid jwt signature, variant c1")
    @Requirement("PIDI-234")
    @XrayTest(key = "PIDI-701")
    void test019() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder.validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withInvalidProof(FLOW_VARIANT, clientId, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT signature is invalid"));
    }

    @Test
    @DisplayName("Credential request invalid dpop, variant c1")
    @Requirement("PIDI-246")
    @XrayTest(key = "PIDI-743")
    void test020() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, "Foo", tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", containsString("error=\"use_dpop_nonce\""))
                .header("WWW-Authenticate", containsString("error_description=\"DPoP nonce is invalid\""))
                .header("WWW-Authenticate", not(containsString("DPoP-Nonce:")))
                .header("DPoP-Nonce", is(notNullValue()));

    }

    @Test
    @DisplayName("Credential request not parseable jwt, variant c1")
    @Requirement("PIDI-234")
    @XrayTest(key = "PIDI-700")
    void test021() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withInvalidProof()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT could not be parsed"));
    }

    @Test
    @DisplayName("Credential endpoint request without dpop, variant c1")
    @Requirement("PIDI-745")
    @XrayTest(key = "PIDI-812")
    void test022() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .withDpopHeader(FLOW_VARIANT, tokenResponse.get("access_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", containsString("error=\"use_dpop_nonce\""))
                .header("WWW-Authenticate", containsString("error_description=\"nonce value missing\""))
                .header("WWW-Authenticate", not(containsString("DPoP-Nonce:")))
                .header("DPoP-Nonce", notNullValue());

    }

    @Test
    @DisplayName("Credential request, happy path with mdoc, variant c1")
    @Requirement({"PIDI-316", "PIDI-421"})
    @XrayTest(key = "PIDI-1054")
    void test023() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validMdoc(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credential", is(notNullValue()))
                .body("c_nonce", is(notNullValue()))
                .body("c_nonce_expires_in", is(notNullValue()))
        ;
    }

    @Test
    @DisplayName("Credential request, unsupported doctype, variant c1")
    @Requirement("PIDI-316")
    @XrayTest(key = "PIDI-1055")
    void test024() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        var body = CredentialRequestBuilder.validMdocRequestBody();
        body.put("doctype", "unsupported");
        CredentialRequestBuilder
                .validMdoc(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_credential_type"))
                .body("error_description", is("Credential type \"unsupported\" not supported"))
        ;
    }

    @Test
    @DisplayName("Credential request, missing doctype, variant c1")
    @Requirement("PIDI-316")
    @XrayTest(key = "PIDI-1056")
    void test025() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validMdoc(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .withRemovedJsonBodyProperty("doctype")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("doctype parameter is missing"))
        ;
    }

    @Test
    @DisplayName("Credential request, missing format, variant c1")
    @Requirement("PIDI-316")
    @XrayTest(key = "PIDI-1057")
    void test027() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validMdoc(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .withRemovedJsonBodyProperty("format")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("format parameter is missing"))
        ;
    }

    @Test
    @DisplayName("Credential request, unsupported format, variant c1")
    @Requirement("PIDI-316")
    @XrayTest(key = "PIDI-1058")
    void test026() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        var body = CredentialRequestBuilder.validMdocRequestBody();
        body.put("format", "unsupported");
        CredentialRequestBuilder
                .validMdoc(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_credential_format"))
                .body("error_description", is("Credential format \"unsupported\" not supported"))
        ;
    }

    @Test
    @DisplayName("Credential request all claims check, variant c1")
    @Requirement({"PIDI-251","PIDI-2202"})
    @XrayTest(key = "PIDI-1126")
    @SuppressWarnings("java:S5961") // checking the credential requires more than 25 assertions
    void test030() {
        java.util.Base64.Decoder urlDecoder = java.util.Base64.getUrlDecoder();
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        var body = CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
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

    @Test
    @DisplayName("Credential request mdoc claims check, variant c1")
    @Requirement({"PIDI-316", "PIDI-421", "PIDI-2287"})
    @XrayTest(key = "PIDI-1666")
    void test031() throws IOException {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));

        var credentialResponse = CredentialRequestBuilder
                .validMdoc(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then();

        var jsonPathObj = credentialResponse.extract().body().jsonPath();
        String credential = jsonPathObj.getString("credential");
        byte[] decodedMDoc = java.util.Base64.getUrlDecoder().decode(credential);
        Map<String, CBORObject> mdocIssuerAuth = readMdocIssuerAuth(decodedMDoc);
        Map<String, Object> mdocPidValues = readPidValues(decodedMDoc);
        String statusUri = mdocIssuerAuth.get("statusUri").AsString();
        assertAll(
                () -> assertTypeIsStandardDateTimeString(mdocIssuerAuth.get("signed")),
                () -> assertTypeIsStandardDateTimeString(mdocIssuerAuth.get("validFrom")),
                () -> assertTypeIsStandardDateTimeString(mdocIssuerAuth.get("validUntil")),
                () -> assertThat(mdocIssuerAuth.get("statusIndex").AsInt32()).isNotNegative(),
                () -> assertThat(statusUri).isNotNull().matches("http.*"),
                () -> assertThat(statusUri.substring(statusUri.lastIndexOf('/') + 1)).matches(ID_REGEX),
                () -> assertThat(mdocPidValues.get("family_name")).hasToString("MUSTERMANN"),
                () -> assertThat(mdocPidValues.get("given_name")).hasToString("ERIKA"),
                () -> assertThat(mdocPidValues.get("birth_date")).hasToString("1964-08-12T00:00:00Z"),
                () -> assertEquals(true, mdocPidValues.get("age_over_12")),
                () -> assertEquals(true, mdocPidValues.get("age_over_14")),
                () -> assertEquals(true, mdocPidValues.get("age_over_18")),
                () -> assertEquals(true, mdocPidValues.get("age_over_21")),
                () -> assertEquals(false, mdocPidValues.get("age_over_65")),
                () -> assertEquals("60", mdocPidValues.get("age_in_years").toString()),
                () -> assertEquals("BERLIN", mdocPidValues.get("birth_place").toString()),
                () -> assertThat(mdocPidValues.get("age_birth_year")).hasToString("1964"),
                () -> assertThat(mdocPidValues.get("family_name_birth")).hasToString("GABLER"),
                () -> assertThat(mdocPidValues.get("resident_address")).isNull(),
                () -> assertEquals("DE", mdocPidValues.get("resident_country").toString()),
                () -> assertThat(mdocPidValues.get("resident_state")).hasToString(""),
                () -> assertEquals("KÖLN", mdocPidValues.get("resident_city").toString()),
                () -> assertEquals("51147", mdocPidValues.get("resident_postal_code").toString()),
                () -> assertEquals("HEIDESTRAẞE 17", mdocPidValues.get("resident_street").toString()),
                () -> assertThat(mdocPidValues.get("nationality")).hasToString("DE"),
                () -> assertThat(mdocPidValues.get("issuing_authority")).hasToString("DE"),
                () -> assertThat(mdocPidValues.get("issuing_country")).hasToString("DE"));
    }

    private static void assertTypeIsStandardDateTimeString(CBORObject cborObject) {
        assertThat(cborObject.getType()).isEqualTo(CBORType.TextString);
        assertThat(cborObject.isTagged()).isTrue();
        assertThat(cborObject.getMostInnerTag().ToInt32Checked()).isZero();
        assertThatNoException().isThrownBy(() -> Instant.parse(cborObject.AsString()));
    }

    @Test
    @DisplayName("Credential request sd-jwt using proof and proofs, variant c1")
    @Requirement({"PIDI-708"})
    @XrayTest(key = "PIDI-2239")
    void test032() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .withProofs(FLOW_VARIANT, clientId, Instant.now(), tokenResponse.get("c_nonce"), 2)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof and proofs MUST NOT be set at the same time."));
    }

    @Test
    @DisplayName("Credential request sd-jwt empty jwt list, variant c1")
    @Requirement({"PIDI-708"})
    @XrayTest(key = "PIDI-2238")
    void test034() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        var body = objectMapper.createObjectNode();
        body.putObject("proofs").putArray("jwt");
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProofs(FLOW_VARIANT, clientId, Instant.now(), tokenResponse.get("c_nonce"), 2)
                .withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof is missing"));
    }

    @Test
    @DisplayName("Credential request sd-jwt missing jwt list, variant c1")
    @Requirement({"PIDI-708"})
    @XrayTest(key = "PIDI-2245")
    void test035() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        var body = objectMapper.createObjectNode();
        body.putObject("proofs");
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProofs(FLOW_VARIANT, clientId, Instant.now(), tokenResponse.get("c_nonce"), 2)
                .withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT could not be parsed"));
    }

    @Test
    @DisplayName("Credential request happy path with proofs, variant c1")
    @Requirement({"PIDI-708"})
    @XrayTest(key = "PIDI-2243")
    void test036() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProofs(FLOW_VARIANT, clientId, Instant.now(), tokenResponse.get("c_nonce"), 2)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK).header("Content-Type", is("application/json"))
                .body("credentials", is(notNullValue()))
                .body("c_nonce", is(notNullValue()))
                .body("c_nonce", matchesPattern(TestUtils.NONCE_REGEX))
                .body("c_nonce_expires_in", isA(Integer.class));
    }

    @Test
    @DisplayName("Credential request dpop nonce expired, variant c1")
    @SuppressWarnings("java:S2925")
    @Requirement({"PIDI-266"})
    @XrayTest(key = "PIDI-2329")
    void test037() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        CredentialRequestBuilder
                .validSdJwt(FlowVariant.C1, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FlowVariant.C1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", containsString("error=\"invalid_dpop_proof\""))
                .header("WWW-Authenticate", containsString("error_description=\"DPoP nonce is expired\""));
    }

    @DisplayName("Kids in jwt-vc-issuer metadata match, variant c1")
    @Test
    @Requirement({"PIDI-401", "PIDI-1397"})
    @XrayTest(key = "PIDI-2526")
    void test038() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        var body = CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest().body();
        var jsonPath = body.jsonPath();
        String[] splitString = jsonPath.getString("credential").split("\\.");
        String base64EncodedBody = splitString[0];
        String jsonString = new String(Base64.decodeBase64(base64EncodedBody), StandardCharsets.UTF_8);
        StringReader reader = new StringReader(jsonString);
        JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
        String issuerKid = jsonObject.get("kid").getAsString();

        new MetadataRequestBuilder()
                .withUrl("/c1/.well-known/jwt-vc-issuer")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("issuer", matchesPattern(BASE_URL_REGEX + "/c1"))
                .body("jwks.keys", hasSize(1))
                .body("jwks.keys[0].exp", isA(Integer.class))
                .body("jwks.keys[0]", hasKey("kid"))
                .body("jwks.keys[0].kid", is(issuerKid));
    }
}
