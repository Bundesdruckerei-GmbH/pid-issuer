/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.PushedAuthorizationRequestBuilder;
import de.bdr.pidi.testdata.TestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.bdr.pidi.testdata.TestUtils.ISSUER_IDENTIFIER_AUDIENCE;
import static de.bdr.pidi.testdata.TestUtils.getClientInstanceKeyMap;
import static java.time.Instant.now;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;

class B1PARTest extends RemoteTest {

    @Test
    @DisplayName("PAR happy path, variant b1")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1227")
    void test001() {
        PushedAuthorizationRequestBuilder.valid()
                .withUrl("/b1/par")
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
    @DisplayName("PAR missing response_type, variant b1")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1257")
    void test002() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withoutResponseType()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'response_type'"));
    }

    @Test
    @DisplayName("PAR missing client_id, variant b1")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1247")
    void test003() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withoutClientId()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'client_id'"));
    }

    @Test
    @DisplayName("PAR malformed redirect_uri, variant b1")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1277")
    void test004() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withRedirectUri("invalid uri")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid redirect URI"));
    }

    @Test
    @DisplayName("PAR missing redirect_uri, variant b1")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1268")
    void test005() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withoutRedirectUri()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'redirect_uri'"));
    }

    @Test
    @DisplayName("PAR malformed response type, variant b1")
//  @Requirement("PIDI-16")
//  @XrayTest(key = "PIDI-264")
    void test006() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withResponseType("invalid response type")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_response_type"))
                .body("error_description", is("Unsupported response type: invalid response type"));
    }

    @Test
    @DisplayName("PAR malformed client_id, variant b1")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1289")
    void test007() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientId("invalid client id")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid client id"));
    }

    @Test
    @DisplayName("PAR missing scope, variant b1")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1180")
    void test008() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withoutScope()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'scope'"));
    }

    @Test
    @DisplayName("PAR malformed scope, variant b1")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1177")
    void test009() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withScope("invalidScope")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_scope"))
                .body("error_description", is("Scopes invalidScope not granted"));
    }

    @Test
    @DisplayName("PAR invalid state, variant b1")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1217")
    void test010() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withState(RandomStringUtils.random(2049))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("error", is("invalid_request"))
                .body("error_description", is("The state parameter exceeds the maximum permitted size of 2048 bytes"));
    }

    @Test
    @DisplayName("PAR client not registered, variant b1")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1244")
    void test011() {
        String uuidAsString = UUID.randomUUID().toString();
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientId(uuidAsString)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client Id not registered: " + uuidAsString));
    }

    @Test
    @DisplayName("PAR missing code_challenge, variant b1")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-1232")
    void test012() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withoutCodeChallenge()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'code_challenge'"));
    }

    @Test
    @DisplayName("PAR missing code_challenge_method, variant b1")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-1262")
    void test013() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withoutCodeChallengeMethod()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'code_challenge_method'"));
    }

    @Test
    @DisplayName("PAR malformed code_challenge, variant b1")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-1253")
    void test014() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid code challenge"));
    }

    @Test
    @DisplayName("PAR malformed code_challenge_method, variant b1")
     @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-1283")
    void test015() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withCodeChallengeMethod("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid code challenge method"));
    }

    @Test
    @DisplayName("PAR missing client assertion, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1272")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test016() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withoutClientAssertion()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion is missing"));
    }

    @Test
    @DisplayName("PAR missing client assertion type, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1306")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test017() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withoutClientAssertionType()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion type is missing"));
    }

    @Test
    @DisplayName("PAR malformed client assertion type, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1295")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test018() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertionType("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion type is invalid"));
    }

    @Test
    @DisplayName("PAR malformed client assertion invalid lenght, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1183")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test019() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAssertion(FlowVariant.B1) + "~invalid~")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion length is invalid"));
    }

    @Test
    @DisplayName("PAR malformed client assertion invalid jwt, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1201")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test020() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAssertion(FlowVariant.B))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR empty client assertion, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1221")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test021() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion is empty"));
    }

    @Test
    @DisplayName("PAR empty client assertion type, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1215")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test022() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertionType("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion type is empty"));
    }

    @Test
    @DisplayName("PAR invalid client attestation signature, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1236")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test023() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "foo~" + TestUtils.getValidClientAssertion(FlowVariant.B1).split("~")[1])
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation signature verification failed"));
    }

    @Test
    @DisplayName("PAR invalid attestation issuer, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1226")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test024() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR invalid signature, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1259")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test025() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getInvalidClientAttestationJwt().serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, An error occurred while checking the signature in client attestation jwt"));
    }

    @Test
    @DisplayName("PAR client assertion not parseable, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1246")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test026() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getClientAttestationJwt(null, "subject", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())) + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion could not be parsed"));
    }

    @Test
    @DisplayName("PAR client assertion missing issuer, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1278")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test027() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getClientAttestationJwt(null, "subject", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty issuer, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1267")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test028() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getClientAttestationJwt("", "subject", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing subject, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1300")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test029() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", null, now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, sub claim is missing"));
    }

    @Test
    @DisplayName("PAR client empty subject, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1211")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test030() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, sub claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing expiration time, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1205")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test031() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", null, now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, exp claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty claim name, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1223")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test032() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), "", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, cnf claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing claim name, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1218")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test033() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), null, Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, cnf claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing claim value, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1242")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test034() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), "cnf", null).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, cnf claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty claim value, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1233")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test035() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), "cnf", "").serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is corrupted"));
    }

    @Test
    @DisplayName("PAR client assertion invalid not before time, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1263")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test036() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now().plus(1, ChronoUnit.DAYS), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is not yet valid"));
    }

    @Test
    @DisplayName("PAR client assertion issued time in future, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1252")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test037() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now().plus(1, ChronoUnit.DAYS), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is issued in the future"));
    }

    @Test
    @DisplayName("PAR client assertion issued time too old, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1285")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test038() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now().minus(1, ChronoUnit.DAYS), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuance is too old"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing issuer, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1273")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test039() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt(null, now(), now(), now(), List.of("audience"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt empty issuer, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1193")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test040() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("", now(), now(), now(), List.of("audience"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid issuer, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1185")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test041() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("f00", now(), now(), now(), List.of("cnf"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer 'f00' does not match the client id 'fed79862-af36-4fee-8e64-89e3c91091ed'"));
    }

    @Test
    @DisplayName("PAR client assertion jwt invalid uuid subject, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1208")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test042() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", UUID.randomUUID().toString(), now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Invalid UUID string: issuer"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid audience, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1200")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test043() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing audience, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1220")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test044() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of(), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, aud claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt empty audience, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1214")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test045() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of(""), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt empty jwtId, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1237")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test046() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), "").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, jti claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing jwtId, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1225")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test047() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), null).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, jti claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid jwtId, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1258")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test048() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), "invalid").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing expiry time, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1248")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test049() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", null, now(), now(), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/b1"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, exp claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid not before, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1178")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test050() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now().plus(1, ChronoUnit.DAYS), now(), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/b1"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is not yet valid"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt issued time in future, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1197")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test051() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now().plus(1, ChronoUnit.DAYS), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/b1" + FlowVariant.B), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is issued in the future"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt issued time too old, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1189")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test052() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now().minus(1, ChronoUnit.DAYS), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/b1"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuance is too old"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid root url, variant b1")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1212")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test053() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b1/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt(FlowVariant.B1, "example.com").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }
}
