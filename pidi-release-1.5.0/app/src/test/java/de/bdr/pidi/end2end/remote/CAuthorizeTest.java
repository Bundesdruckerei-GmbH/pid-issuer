/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.AuthorizationRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import de.bdr.pidi.testdata.ClientIds;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;

class CAuthorizeTest extends RemoteTest {
    private final Steps steps = new Steps(FlowVariant.C);

    @DisplayName("Authorize happy path, variant c")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-269")
    void test001(){
        String requestUri = steps.doPAR();
        new AuthorizationRequestBuilder("/c/authorize")
                .withClientId(ClientIds.validClientId().toString())
                .withRequestUri(requestUri)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.SEE_OTHER)
                .header("Location", containsString("SAMLRequest="));
    }

    @DisplayName("Authorize missing client_id, variant c")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-353")
    void test002(){
        String requestUri = steps.doPAR();
        new AuthorizationRequestBuilder("/c/authorize")
                .withRequestUri(requestUri)
                .doRequest()
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'client_id'"));
    }

    @DisplayName("Authorize missing request_uri, variant c")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-355")
    void test003(){
        new AuthorizationRequestBuilder("/c/authorize")
                .withClientId(ClientIds.validClientId().toString())
                .doRequest()
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid request_uri"));
    }

    @DisplayName("Authorize malformed request_uri, variant c")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-357")
    void test004(){
        new AuthorizationRequestBuilder("/c/authorize")
                .withClientId(ClientIds.validClientId().toString())
                .withRequestUri("invalid uri")
                .doRequest()
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid request_uri"));
    }

    @DisplayName("Authorize malformed client_id, variant c")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-359")
    void test005(){
        String requestUri = steps.doPAR();
        new AuthorizationRequestBuilder("/c/authorize")
                .withClientId("123456")
                .withRequestUri(requestUri)
                .doRequest()
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid client id"));
    }

    @DisplayName("Authorize empty client_id, variant c")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-373")
    void test006(){
        String requestUri = steps.doPAR();
        new AuthorizationRequestBuilder("/c/authorize")
                .withClientId("")
                .withRequestUri(requestUri)
                .doRequest()
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid client id"));
    }

    @DisplayName("Authorize request_uri do not references an existing session, variant c")
    @Test
    @Requirement({"PIDI-299","PIDI-17"})
    @XrayTest(key = "PIDI-376")
    void test007(){
        steps.doPAR();
        new AuthorizationRequestBuilder("/c/authorize")
                .withClientId(ClientIds.validClientId().toString())
                .withRequestUri("urn:ietf:params:oauth:request_uri:foo")
                .doRequest()
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid request_uri"));
    }

}
