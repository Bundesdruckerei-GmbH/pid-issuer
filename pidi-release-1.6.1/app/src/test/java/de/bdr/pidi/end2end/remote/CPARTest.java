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

class CPARTest extends RemoteTest {

    @Test
    @DisplayName("PAR happy path, variant c")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-187")
    void test001() {
        PushedAuthorizationRequestBuilder.valid()
                .withUrl("/c/par")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.CREATED)
                .body("expires_in", is(60))
                .body("request_uri", startsWith("urn:ietf:params:oauth:request_uri:"))
                .body("request_uri", hasLength(56));
    }


    @Test
    @DisplayName("PAR missing client_id, variant c")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-211")
    void test002() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withoutClientId()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'client_id'"));
    }

    @Test
    @DisplayName("PAR malformed client_id, variant c")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-185")
    void test003() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientId("This should be an UUID")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid client id"));
    }

    @Test
    @DisplayName("PAR missing response_type, variant c")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-210")
    void test004() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withoutResponseType()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'response_type'"));
    }

    @Test
    @DisplayName("PAR malformed response_type, variant c")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-190")
    void test005() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withResponseType("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_response_type"))
                .body("error_description", is("Unsupported response type: invalid"));
    }


    @Test
    @DisplayName("PAR malformed redirect_uri, variant c")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-254")
    void test006() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withRedirectUri("invalid uri")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid redirect URI"));
    }

    @Test
    @DisplayName("PAR missing redirect_uri, variant c")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-253")
    void test007() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withoutRedirectUri()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'redirect_uri'"));
    }

    @Test
    @DisplayName("PAR missing scope, variant c")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-252")
    void test008() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withoutScope()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'scope'"));
    }

    @Test
    @DisplayName("PAR malformed scope, variant c")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-255")
    void test009() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withScope("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_scope"))
                .body("error_description", is("Scopes invalid not granted"));
    }

    @Test
    @DisplayName("PAR invalid state, variant c")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-290")
    void test010() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withState(RandomStringUtils.insecure().next(2049))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("error", is("invalid_request"))
                .body("error_description", is("The state parameter exceeds the maximum permitted size of 2048 bytes"));
    }

    @Test
    @DisplayName("PAR client not registered, variant c")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-296")
    void test011() {
        String uuidAsString = UUID.randomUUID().toString();
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientId(uuidAsString)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client Id not registered: " + uuidAsString));
    }

    @Test
    @DisplayName("PAR missing code_challenge, variant c")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-314")
    void test012() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withoutCodeChallenge()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'code_challenge'"));
    }

    @Test
    @DisplayName("PAR missing code_challenge_method, variant c")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-313")
    void test013() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withoutCodeChallengeMethod()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'code_challenge_method'"));
    }

    @Test
    @DisplayName("PAR malformed code_challenge, variant c")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-328")
    void test014() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withCodeChallenge("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid code challenge"));
    }

    @Test
    @DisplayName("PAR malformed code_challenge_method, variant c")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-329")
    void test015() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withCodeChallengeMethod("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid code challenge method"));
    }

    @Test
    @DisplayName("PAR missing client assertion, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-432")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test016() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withoutClientAssertion()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion is missing"));
    }

    @Test
    @DisplayName("PAR missing client assertion type, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-430")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test017() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withoutClientAssertionType()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion type is missing"));
    }

    @Test
    @DisplayName("PAR malformed client assertion type, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-431")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test018() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertionType("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion type is invalid"));
    }

    @Test
    @DisplayName("PAR malformed client assertion invalid lenght, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-433")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test019() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAssertion(FlowVariant.C) + "~invalid~")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion length is invalid"));
    }

    @Test
    @DisplayName("PAR malformed client assertion invalid jwt, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-442")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test020() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAssertion(FlowVariant.C1))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR empty client assertion, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-435")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test021() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion is empty"));
    }

    @Test
    @DisplayName("PAR empty client assertion type, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-444")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test022() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertionType("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion type is empty"));
    }

    @Test
    @DisplayName("PAR invalid client attestation signature, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-452")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test023() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "foo~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation signature verification failed"));
    }

    @Test
    @DisplayName("PAR invalid attestation issuer, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-451")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test024() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C1).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR invalid signature, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-467")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test025() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getInvalidClientAttestationJwt().serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, An error occurred while checking the signature in client attestation jwt"));
    }

    @Test
    @DisplayName("PAR client assertion not parseable, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-468")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test026() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt(null, "subject", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())) + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion could not be parsed"));
    }

    @Test
    @DisplayName("PAR client assertion missing issuer, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-470")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test027() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt(null, "subject", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty issuer, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-471")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test028() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("", "subject", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing subject, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-472")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test029() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", null, now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, sub claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty subject, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-473")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test030() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, sub claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing expiration time, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-475")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test031() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", null, now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, exp claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty claim name, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-476")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test032() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), "", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, cnf claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing claim name, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-477")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test033() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), null, Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, cnf claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing claim value, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-478")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test034() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), "cnf", null).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, cnf claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty claim value, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-480")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test035() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), "cnf", "").serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is corrupted"));
    }

    @Test
    @DisplayName("PAR client assertion invalid not before time, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-481")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test036() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now().plus(1, ChronoUnit.DAYS), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is not yet valid"));
    }

    @Test
    @DisplayName("PAR client assertion invalid issued time in future, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-482")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test037() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now().plus(1, ChronoUnit.DAYS), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is issued in the future"));
    }

    @Test
    @DisplayName("PAR client assertion invalid issued time too old, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-483")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test038() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now().minus(1, ChronoUnit.DAYS), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuance is too old"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing issuer, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-497")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test039() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt(null, now(), now(), now(), List.of("audience"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt empty issuer, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-506")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test040() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("", now(), now(), now(), List.of("audience"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid issuer, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-507")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test041() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("f00", now(), now(), now(), List.of("cnf"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer 'f00' does not match the client id 'fed79862-af36-4fee-8e64-89e3c91091ed'"));
    }

    @Test
    @DisplayName("PAR client assertion jwt invalid uuid in subject, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-508")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test042() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", UUID.randomUUID().toString(), now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Invalid UUID string: issuer"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid audience, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-509")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test043() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing audience, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-510")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test044() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of(), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, aud claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt empty audience, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-511")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test045() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of(""), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt empty jwtId, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-512")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test046() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), "").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, jti claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing jwtId, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-513")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test047() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), null).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, jti claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid jwtId, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-514")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test048() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), "invalid").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing expiry time, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-515")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test049() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", null, now(), now(), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/c"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, exp claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid not before, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-517")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test050() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now().plus(1, ChronoUnit.DAYS), now(), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/c"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is not yet valid"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt issued time in future, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-518")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test051() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now().plus(1, ChronoUnit.DAYS), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/c"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is issued in the future"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt issued time too old, variant c")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-519")
    @Disabled("PIDI-739: Temporarily disable Client Attestation for Variant C")
    void test052() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now().minus(1, ChronoUnit.DAYS), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/c"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuance is too old"));
    }
}
