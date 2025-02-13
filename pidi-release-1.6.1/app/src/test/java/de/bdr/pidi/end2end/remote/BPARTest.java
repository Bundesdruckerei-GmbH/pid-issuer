/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
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

class BPARTest extends RemoteTest {

    @Test
    @DisplayName("PAR happy path, variant b")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-888")
    void test001() {
        PushedAuthorizationRequestBuilder.valid()
                .withUrl("/b/par")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.CREATED)
                .body("expires_in", is(60))
                .body("request_uri", startsWith("urn:ietf:params:oauth:request_uri:"))
                .body("request_uri", hasLength(56));
    }

    @Test
    @DisplayName("PAR missing client_id, variant b")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-899")
    void test002() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withoutClientId()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'client_id'"));
    }

    @Test
    @DisplayName("PAR malformed client_id, variant b")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-912")
    void test003() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientId("This should be an UUID")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid client id"));
    }

    @Test
    @DisplayName("PAR missing response_type, variant b")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-926")
    void test004() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withoutResponseType()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'response_type'"));
    }

    @Test
    @DisplayName("PAR malformed response_type, variant b")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-940")
    void test005() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withResponseType("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_response_type"))
                .body("error_description", is("Unsupported response type: invalid"));
    }

    @Test
    @DisplayName("PAR malformed redirect_uri, variant b")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-954")
    void test006() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withRedirectUri("invalid uri")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid redirect URI"));
    }

    @Test
    @DisplayName("PAR missing redirect_uri, variant b")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-866")
    void test007() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withoutRedirectUri()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'redirect_uri'"));
    }

    @Test
    @DisplayName("PAR missing scope, variant b")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-877")
    void test008() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withoutScope()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'scope'"));
    }

    @Test
    @DisplayName("PAR malformed scope, variant b")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-887")
    void test009() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withScope("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_scope"))
                .body("error_description", is("Scopes invalid not granted"));
    }

    @Test
    @DisplayName("PAR invalid state, variant b")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-861")
    void test010() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withState(RandomStringUtils.insecure().next(2049))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("error", is("invalid_request"))
                .body("error_description", is("The state parameter exceeds the maximum permitted size of 2048 bytes"));
    }

    @Test
    @DisplayName("PAR client not registered, variant b")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-874")
    void test011() {
        String uuidAsString = UUID.randomUUID().toString();
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientId(uuidAsString)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client Id not registered: " + uuidAsString));
    }

    @Test
    @DisplayName("PAR missing code_challenge, variant b")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-884")
    void test012() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withoutCodeChallenge()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'code_challenge'"));
    }

    @Test
    @DisplayName("PAR missing code_challenge_method, variant b")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-896")
    void test013() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withoutCodeChallengeMethod()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'code_challenge_method'"));
    }

    @Test
    @DisplayName("PAR malformed code_challenge, variant b")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-907")
    void test014() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withCodeChallenge("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid code challenge"));
    }

    @Test
    @DisplayName("PAR malformed code_challenge_method, variant b")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-918")
    void test015() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withCodeChallengeMethod("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid code challenge method"));
    }

    @Test
    @DisplayName("PAR missing client assertion, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-930")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test016() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withoutClientAssertion()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion is missing"));
    }

    @Test
    @DisplayName("PAR missing client assertion type, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-947")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test017() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withoutClientAssertionType()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion type is missing"));
    }

    @Test
    @DisplayName("PAR malformed client assertion type, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-858")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test018() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertionType("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion type is invalid"));
    }

    @Test
    @DisplayName("PAR malformed client assertion invalid lenght, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-871")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test019() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAssertion(FlowVariant.B) + "~invalid~")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion length is invalid"));
    }

    @Test
    @DisplayName("PAR malformed client assertion invalid jwt, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-943")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test020() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAssertion(FlowVariant.B1))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR empty client assertion, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-956")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test021() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion is empty"));
    }

    @Test
    @DisplayName("PAR empty client assertion type, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-864")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test022() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertionType("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion type is empty"));
    }

    @Test
    @DisplayName("PAR invalid client attestation signature, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-878")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test023() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "foo~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation signature verification failed"));
    }

    @Test
    @DisplayName("PAR invalid attestation issuer, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-889")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test024() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR invalid signature, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-901")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test025() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getInvalidClientAttestationJwt().serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, An error occurred while checking the signature in client attestation jwt"));
    }

    @Test
    @DisplayName("PAR client assertion not parseable, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-911")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test026() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt(null, "subject", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())) + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion could not be parsed"));
    }

    @Test
    @DisplayName("PAR client assertion missing issuer, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-924")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test027() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt(null, "subject", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty issuer, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-938")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test028() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("", "subject", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing subject, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-952")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test029() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", null, now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, sub claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty subject, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-921")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test030() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, sub claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing expiration time, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-933")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test031() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", null, now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, exp claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty claim name, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-949")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test032() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), "", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, cnf claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing claim name, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-859")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test033() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), null, Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, cnf claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing claim value, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-873")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test034() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), "cnf", null).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, cnf claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty claim value, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-885")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test035() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), "cnf", "").serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is corrupted"));
    }

    @Test
    @DisplayName("PAR client assertion invalid not before time, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-895")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test036() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now().plus(1, ChronoUnit.DAYS), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is not yet valid"));
    }

    @Test
    @DisplayName("PAR client assertion invalid issued time in future, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-905")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test037() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now().plus(1, ChronoUnit.DAYS), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is issued in the future"));
    }

    @Test
    @DisplayName("PAR client assertion invalid issued time too old, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-919")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test038() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now().minus(1, ChronoUnit.DAYS), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuance is too old"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing issuer, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-931")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test039() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt(null, now(), now(), now(), List.of("audience"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt empty issuer, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-902")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test040() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("", now(), now(), now(), List.of("audience"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid issuer, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-914")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test041() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("f00", now(), now(), now(), List.of("cnf"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer 'f00' does not match the client id 'fed79862-af36-4fee-8e64-89e3c91091ed'"));
    }

    @Test
    @DisplayName("PAR client assertion jwt invalid uuid in subject, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-927")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test042() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", UUID.randomUUID().toString(), now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.B).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Invalid UUID string: issuer"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid audience, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-944")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test043() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing audience, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-953")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test044() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of(), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, aud claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt empty audience, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-865")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test045() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of(""), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt empty jwtId, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-879")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test046() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), "").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, jti claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing jwtId, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-890")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test047() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), null).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, jti claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid jwtId, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-898")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test048() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), "invalid").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing expiry time, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-910")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test049() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", null, now(), now(), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/c"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, exp claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid not before, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-886")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test050() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now().plus(1, ChronoUnit.DAYS), now(), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/c"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is not yet valid"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt issued time in future, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-897")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test051() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now().plus(1, ChronoUnit.DAYS), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/c"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is issued in the future"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt issued time too old, variant b")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-908")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test052() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/b/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now().minus(1, ChronoUnit.DAYS), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/c"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuance is too old"));
    }
}
