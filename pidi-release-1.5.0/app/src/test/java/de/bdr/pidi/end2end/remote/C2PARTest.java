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

class C2PARTest extends RemoteTest {

    @Test
    @DisplayName("PAR happy path, variant c2")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1587")
    void test001() {
        PushedAuthorizationRequestBuilder.valid()
                .withUrl("/c2/par")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.CREATED)
                .body("expires_in", is(60))
                .body("request_uri", startsWith("urn:ietf:params:oauth:request_uri:"))
                .body("request_uri", hasLength(56));
    }

    @Test
    @DisplayName("PAR missing client_id, variant c2")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1569")
    void test002() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withoutClientId()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'client_id'"));
    }

    @Test
    @DisplayName("PAR malformed client_id, variant c2")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1559")
    void test003() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientId("This should be an UUID")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid client id"));
    }

    @Test
    @DisplayName("PAR missing response_type, variant c2")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1550")
    void test004() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withoutResponseType()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'response_type'"));
    }

    @Test
    @DisplayName("PAR malformed response_type, variant c2")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1542")
    void test005() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withResponseType("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_response_type"))
                .body("error_description", is("Unsupported response type: invalid"));
    }

    @Test
    @DisplayName("PAR malformed redirect_uri, variant c2")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1535")
    void test006() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withRedirectUri("invalid uri")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid redirect URI"));
    }

    @Test
    @DisplayName("PAR missing redirect_uri, variant c2")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1526")
    void test007() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withoutRedirectUri()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'redirect_uri'"));
    }

    @Test
    @DisplayName("PAR missing scope, variant c2")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1650")
    void test008() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withoutScope()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'scope'"));
    }

    @Test
    @DisplayName("PAR malformed scope, variant c2")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1641")
    void test009() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withScope("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_scope"))
                .body("error_description", is("Scopes invalid not granted"));
    }

    @Test
    @DisplayName("PAR invalid state, variant c2")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1598")
    void test010() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withState(RandomStringUtils.random(2049))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("error", is("invalid_request"))
                .body("error_description", is("The state parameter exceeds the maximum permitted size of 2048 bytes"));
    }

    @Test
    @DisplayName("PAR client not registered, variant c2")
    @Requirement("PIDI-16")
    @XrayTest(key = "PIDI-1589")
    void test011() {
        String uuidAsString = UUID.randomUUID().toString();
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientId(uuidAsString)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client Id not registered: " + uuidAsString));
    }

    @Test
    @DisplayName("PAR missing code_challenge, variant c2")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-1582")
    void test012() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withoutCodeChallenge()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'code_challenge'"));
    }

    @Test
    @DisplayName("PAR missing code_challenge_method, variant c2")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-1573")
    void test013() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withoutCodeChallengeMethod()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'code_challenge_method'"));
    }

    @Test
    @DisplayName("PAR malformed code_challenge, variant c2")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-1564")
    void test014() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withCodeChallenge("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid code challenge"));
    }

    @Test
    @DisplayName("PAR malformed code_challenge_method, variant c2")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-1554")
    void test015() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withCodeChallengeMethod("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid code challenge method"));
    }

    @Test
    @DisplayName("PAR missing client assertion, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1547")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test016() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withoutClientAssertion()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion is missing"));
    }

    @Test
    @DisplayName("PAR missing client assertion type, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1539")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test017() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withoutClientAssertionType()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion type is missing"));
    }

    @Test
    @DisplayName("PAR malformed client assertion type, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1531")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test018() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertionType("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion type is invalid"));
    }

    @Test
    @DisplayName("PAR malformed client assertion invalid lenght, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1524")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test019() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAssertion(FlowVariant.C2) + "~invalid~")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion length is invalid"));
    }

    @Test
    @DisplayName("PAR malformed client assertion invalid jwt, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1611")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test020() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAssertion(FlowVariant.C))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR empty client assertion, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1603")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test021() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion is empty"));
    }

    @Test
    @DisplayName("PAR empty client assertion type, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1594")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test022() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertionType("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion type is empty"));
    }

    @Test
    @DisplayName("PAR invalid client attestation signature, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1586")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test023() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "foo~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation signature verification failed"));
    }

    @Test
    @DisplayName("PAR invalid attestation issuer, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1577")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test024() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withCodeChallenge("vzB-O3GYo4x7hMksFV4FIvtqW6g8XWLv-sUtdEUIZdE=")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR invalid signature, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1568")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test025() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getInvalidClientAttestationJwt().serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, An error occurred while checking the signature in client attestation jwt"));
    }

    @Test
    @DisplayName("PAR client assertion not parseable, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1560")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test026() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt(null, "subject", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())) + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client assertion could not be parsed"));
    }

    @Test
    @DisplayName("PAR client assertion missing issuer, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1549")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test027() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt(null, "subject", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty issuer, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1541")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test028() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("", "subject", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing subject, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1534")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test029() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", null, now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, sub claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty subject, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1623")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test030() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "", now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, sub claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing expiration time, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1616")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test031() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", null, now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, exp claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty claim name, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1607")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test032() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), "", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, cnf claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing claim name, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1597")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test033() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), null, Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, cnf claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion missing claim value, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1591")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test034() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), "cnf", null).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, cnf claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion empty claim value, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1583")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test035() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now(), "cnf", "").serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is corrupted"));
    }

    @Test
    @DisplayName("PAR client assertion invalid not before time, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1571")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test036() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now().plus(1, ChronoUnit.DAYS), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is not yet valid"));
    }

    @Test
    @DisplayName("PAR client assertion invalid issued time in future, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1563")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test037() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now().plus(1, ChronoUnit.DAYS), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is issued in the future"));
    }

    @Test
    @DisplayName("PAR client assertion invalid issued time too old, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1553")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test038() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", "subject", now(), now(), now().minus(1, ChronoUnit.DAYS), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuance is too old"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing issuer, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1546")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test039() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt(null, now(), now(), now(), List.of("audience"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt empty issuer, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1634")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test040() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("", now(), now(), now(), List.of("audience"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, iss claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid issuer, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1627")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test041() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("f00", now(), now(), now(), List.of("cnf"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer 'f00' does not match the client id 'fed79862-af36-4fee-8e64-89e3c91091ed'"));
    }

    @Test
    @DisplayName("PAR client assertion jwt invalid uuid in subject, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1620")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test042() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getClientAttestationJwt("issuer", UUID.randomUUID().toString(), now(), now(), now(), "cnf", Map.of("jwk", getClientInstanceKeyMap())).serialize() + "~" + TestUtils.getValidClientAttestationPopJwt(FlowVariant.C2))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Invalid UUID string: issuer"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid audience, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1612")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test043() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing audience, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1604")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test044() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of(), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, aud claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt empty audience, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1595")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test045() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of(""), "jwtId").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt empty jwtId, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1584")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test046() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), "").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, jti claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing jwtId, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1574")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test047() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), null).serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, jti claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid jwtId, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1566")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test048() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now(), List.of("cnf"), "invalid").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuer audience unknown"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt missing expiry time, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1558")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test049() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", null, now(), now(), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/c"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, exp claim is missing"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt invalid not before, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1647")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test050() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now().plus(1, ChronoUnit.DAYS), now(), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/c"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is not yet valid"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt issued time in future, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1639")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test051() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now().plus(1, ChronoUnit.DAYS), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/c"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation is issued in the future"));
    }

    @Test
    @DisplayName("PAR client assertion pop jwt issued time too old, variant c2")
    @Requirement("PIDI-168")
    @XrayTest(key = "PIDI-1633")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test052() {
        PushedAuthorizationRequestBuilder.valid().withUrl("/c2/par")
                .withClientAssertion(TestUtils.getValidClientAttestationJwt().serialize() + "~" + TestUtils.getClientAttestationPopJwt("fed79862-af36-4fee-8e64-89e3c91091ed", now(), now(), now().minus(1, ChronoUnit.DAYS), List.of(ISSUER_IDENTIFIER_AUDIENCE + "/c"), "test").serialize())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_client"))
                .body("error_description", is("Client attestation jwt is invalid, Client attestation issuance is too old"));
    }
}
