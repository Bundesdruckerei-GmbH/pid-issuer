/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.eidauthmock.client.model.EidAuthMockEidData;
import de.bdr.pidi.eidauthmock.client.model.EidAuthMockGeneralPlace;
import de.bdr.pidi.eidauthmock.client.model.EidAuthMockPlace;
import de.bdr.pidi.end2end.requests.CredentialRequestBuilder;
import de.bdr.pidi.end2end.requests.MockIdRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static de.bdr.pidi.testdata.TestUtils.readPidValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@Isolated
class C1CredentialMockModTest extends RemoteTest {

    private static final FlowVariant FLOW_VARIANT = FlowVariant.C1;
    private final Steps steps = new Steps(FLOW_VARIANT);

    @Test
    @DisplayName("Credential request mdoc, country code in address from outside Germany check, variant c1")
    @XrayTest(key = "PIDI-2488")
    void test001() throws IOException {
        EidAuthMockEidData data = new EidAuthMockEidData()
                .placeOfResidence(new EidAuthMockGeneralPlace().structuredPlace(
                        // Danish address
                        new EidAuthMockPlace()
                                .country("DNK")
                                .street("BAKKEGÅRDSVEJ 15A")
                                .zipcode("6340")
                                .city("KRUSÅ")
                ));
        var response = new MockIdRequestBuilder().withData(data)
                .doRequest().body().prettyPrint();
        log.debug("mock response: {}", response);

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
        Map<String, Object> mdocPidValues = readPidValues(decodedMDoc);
        assertAll(
                () -> assertEquals("DK", mdocPidValues.get("resident_country").toString()),
                () -> assertThat(mdocPidValues.get("resident_state")).hasToString(""),
                () -> assertEquals("KRUSÅ", mdocPidValues.get("resident_city").toString()),
                () -> assertEquals("6340", mdocPidValues.get("resident_postal_code").toString()),
                () -> assertEquals("BAKKEGÅRDSVEJ 15A", mdocPidValues.get("resident_street").toString()));
    }

    @Test
    @DisplayName("Credential request mdoc, nationality not german, variant c1")
    @XrayTest(key = "PIDI-2486")
    @SuppressWarnings("java:S5976") // each test requires its own XrayTest annotations
    void test002() throws IOException {
        EidAuthMockEidData data = new EidAuthMockEidData()
                .nationality("UKR"); // Ukraine
        var response = new MockIdRequestBuilder().withData(data)
                .doRequest().body().prettyPrint();
        log.debug("mock response: {}", response);

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
        Map<String, Object> mdocPidValues = readPidValues(decodedMDoc);
        assertAll(
                () -> assertEquals("UA", mdocPidValues.get("nationality").toString()));
    }

    @Test
    @DisplayName("Credential request mdoc, person of unspecified, variant c1")
    @ExpectedToFail("Issue in nimbus library")
    @XrayTest(key = "PIDI-2492")
    void test003() throws IOException {
        EidAuthMockEidData data = new EidAuthMockEidData()
                .nationality("XXX"); //  a person of unspecified nationality
        var response = new MockIdRequestBuilder().withData(data)
                .doRequest().body().prettyPrint();
        log.debug("mock response: {}", response);

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
        Map<String, Object> mdocPidValues = readPidValues(decodedMDoc);
        assertAll(
                () -> assertEquals("XX", mdocPidValues.get("nationality").toString()));
    }

    @Test
    @DisplayName("Credential request mdoc, stateless person, variant c1")
    @ExpectedToFail("Issue in nimbus library")
    @XrayTest(key = "PIDI-2490")
    void test004() throws IOException {
        EidAuthMockEidData data = new EidAuthMockEidData()
                .nationality("XXA"); //   a stateless person, as defined in Article 1 of the 1954 Convention Relating to the Status of Stateless
        var response = new MockIdRequestBuilder().withData(data)
                .doRequest().body().prettyPrint();
        log.debug("mock response: {}", response);

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
        Map<String, Object> mdocPidValues = readPidValues(decodedMDoc);
        assertAll(
                () -> assertEquals("XX", mdocPidValues.get("nationality").toString()));
    }

    @Test
    @DisplayName("Credential request mdoc, refuge person, variant c1")
    @ExpectedToFail("Issue in nimbus library")
    @XrayTest(key = "PIDI-2489")
    void test005() throws IOException {
        EidAuthMockEidData data = new EidAuthMockEidData()
                .nationality("XXB"); // a refugee, as defined in Article 1 of the 1951 Convention Relating to the Status of Refugees as amended by the 1967 Protocol
        var response = new MockIdRequestBuilder().withData(data)
                .doRequest().body().prettyPrint();
        log.debug("mock response: {}", response);

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
        Map<String, Object> mdocPidValues = readPidValues(decodedMDoc);
        assertAll(
                () -> assertEquals("XX", mdocPidValues.get("nationality").toString()));
    }

    @Test
    @DisplayName("Credential request mdoc, other refuge person, variant c1")
    @ExpectedToFail("Issue in nimbus library")
    @XrayTest(key = "PIDI-2487")
    void test006() throws IOException {
        EidAuthMockEidData data = new EidAuthMockEidData()
                .nationality("XXC"); // a refugee, in cases not defined above (XXA and XXB)
        var response = new MockIdRequestBuilder().withData(data)
                .doRequest().body().prettyPrint();
        log.debug("mock response: {}", response);

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
        Map<String, Object> mdocPidValues = readPidValues(decodedMDoc);
        assertAll(
                () -> assertEquals("XX", mdocPidValues.get("nationality").toString()));
    }

    @Test
    @DisplayName("Credential request mdoc, nationality kosovar, variant c1")
    @ExpectedToFail("Issue in nimbus library")
    @XrayTest(key = "PIDI-2493")
    void test007() throws IOException {
        EidAuthMockEidData data = new EidAuthMockEidData()
                .nationality("XXK"); // eu code for Kosovo
        var response = new MockIdRequestBuilder().withData(data)
                .doRequest().body().prettyPrint();
        log.debug("mock response: {}", response);

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
        Map<String, Object> mdocPidValues = readPidValues(decodedMDoc);
        assertAll(
                () -> assertEquals("XK", mdocPidValues.get("nationality").toString()));
    }

    @Test
    @DisplayName("Credential request mdoc, invalid nationality code, variant c1")
    @ExpectedToFail("Not fixed yet")
    @XrayTest(key = "PIDI-2491")
    void test008() throws IOException {
        EidAuthMockEidData data = new EidAuthMockEidData()
                .nationality("XYZ"); // invalid code
        var response = new MockIdRequestBuilder().withData(data)
                .doRequest().body().prettyPrint();
        log.debug("mock response: {}", response);

        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));

        CredentialRequestBuilder
                .validMdoc(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then().status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("error", is("Internal server error"))
                .body("error_description", is("PID could not get issued due to a data issue. Please contact the support of Bundesdruckerei GmbH."));

    }
}
