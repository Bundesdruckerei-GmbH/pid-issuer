/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.FinishAuthorizationRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;

class CFinishAuthorizationTest extends RemoteTest {

    private final Steps steps = new Steps(FlowVariant.C);

    @DisplayName("Finish authorization happy path, variant c")
    @Test
    @Requirement({"PIDI-317","PIDI-19", "PIDI-335"})
    @XrayTest(key = "PIDI-283")
    void test001() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        new FinishAuthorizationRequestBuilder("/c/finish-authorization")
                .withIssuerState(issuerState)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.FOUND)
                .header("location", startsWith("https://secure.redirect.com?"))
                .header("Location", containsString("code="))
                .header("dpop-nonce", is(notNullValue()));
    }

    @DisplayName("Finish authorization missing issuer_state, variant c")
    @Test
    @Requirement({"PIDI-317","PIDI-19"})
    @XrayTest(key = "PIDI-377")
    void test002() {
        new FinishAuthorizationRequestBuilder("/c/finish-authorization")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization empty issuer_state, variant c")
    @Test
    @Requirement({"PIDI-317","PIDI-19"})
    @XrayTest(key = "PIDI-381")
    void test003() {
        new FinishAuthorizationRequestBuilder("/c/finish-authorization")
                .withIssuerState("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization malformed issuer_state, variant c")
    @Test
    @Requirement({"PIDI-317","PIDI-19"})
    @XrayTest(key = "PIDI-383")
    void test004() {
        new FinishAuthorizationRequestBuilder("/c/finish-authorization")
                .withIssuerState("?nval~d")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization invalid issuer_state reference, variant c")
    @Test
    @Requirement({"PIDI-317","PIDI-19"})
    @XrayTest(key = "PIDI-402")
    void test005() {
        var requestUri = steps.doPAR();
        steps.doAuthorize(requestUri);
        new FinishAuthorizationRequestBuilder("/c/finish-authorization")
                .withIssuerState(UUID.randomUUID().toString())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization invalid next expected request, variant c")
    @Test
    void test006() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        steps.doFinishAuthorization(issuerState);
        new FinishAuthorizationRequestBuilder("/c/finish-authorization")
                .withIssuerState(issuerState)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.FOUND)
                .header("location", startsWith("https://secure.redirect.com?"))
                .header("Location", containsString("error=invalid_request"));
    }
}
