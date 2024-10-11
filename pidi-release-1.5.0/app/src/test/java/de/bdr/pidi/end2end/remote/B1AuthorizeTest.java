/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.AuthorizationRequestBuilder;
import de.bdr.pidi.end2end.requests.EidRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.TestConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;

class B1AuthorizeTest extends RemoteTest {

    private final Steps steps = new Steps(FlowVariant.B1);

    @DisplayName("Authorize happy path, variant b1")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-1213")
    void test001(){
        String requestUri = steps.doPAR();
        new AuthorizationRequestBuilder("/b1/authorize")
                .withClientId(ClientIds.validClientId().toString())
                .withRequestUri(requestUri)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.SEE_OTHER)
                .header("Location", containsString("SAMLRequest="));
    }
    @DisplayName("Authorize missing client_id, variant b1")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-1206")
    void test002(){
        String requestUri = steps.doPAR();
        new AuthorizationRequestBuilder("/b1/authorize")
                .withRequestUri(requestUri)
                .doRequest()
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'client_id'"));
    }
    @DisplayName("Authorize missing request_uri, variant b1")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-1199")
    void test003(){
        new AuthorizationRequestBuilder("/b1/authorize")
                .withClientId(ClientIds.validClientId().toString())
                .doRequest()
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid request_uri"));
    }
    @DisplayName("Authorize malformed request_uri, variant b1")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-1192")
    void test004(){
        String requestUri = "invalid uri";
        new AuthorizationRequestBuilder("/b1/authorize")
                .withClientId(ClientIds.validClientId().toString())
                .withRequestUri(requestUri)
                .doRequest()
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid request_uri"));
    }
    @DisplayName("Authorize malformed client_id, variant b1")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-1184")
    void test005(){
        String requestUri = steps.doPAR();
        new AuthorizationRequestBuilder("/b1/authorize")
                .withClientId("123456")
                .withRequestUri(requestUri)
                .doRequest()
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid client id"));
    }

    @DisplayName("Authorize empty client_id, variant b1")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-1179")
    void test006(){
        String requestUri = steps.doPAR();
        new AuthorizationRequestBuilder("/b1/authorize")
                .withClientId("")
                .withRequestUri(requestUri)
                .doRequest()
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid client id"));
    }
    @DisplayName("Authorize request_uri do not references an existing session, variant b1")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-1308")
    void test007(){
        new AuthorizationRequestBuilder("/b1/authorize")
                .withClientId(ClientIds.validClientId().toString())
                .withRequestUri("urn:ietf:params:oauth:request_uri:foo123")
                .doRequest()
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid request_uri"));
    }

    @DisplayName("Authorize happy path, variant b1, via mock")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-1297")
    void test008() {
        String requestUri = steps.doPAR();
        String location = new AuthorizationRequestBuilder("/b1/authorize")
                .withClientId(ClientIds.validClientId().toString())
                .withRequestUri(requestUri)
                .getRequestUrl(TestConfig.pidiBaseUrl());

        String finishAuthUrl = new EidRequestBuilder().withTCTokenUrl(location)
                .withPort(TestConfig.getEidMockPort()).withHost(TestConfig.getEidMockHostname()).doRequest().header("Location");

        assertThat(finishAuthUrl, Matchers.startsWith(TestConfig.pidiBaseUrl() + "/b1/finish-authorization?issuer_state"));
    }
}
