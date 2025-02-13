/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
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
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
class BFinishAuthorizationTest extends RemoteTest {

    private final Steps steps = new Steps(FlowVariant.B);

    @DisplayName("Finish authorization happy path, variant b")
    @Test
    @Requirement({"PIDI-317","PIDI-19", "PIDI-335"})
    @XrayTest(key = "PIDI-876")
    void test001() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        new FinishAuthorizationRequestBuilder("/b/finish-authorization")
                .withIssuerState(issuerState)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.FOUND)
                .header("location", startsWith("https://secure.redirect.com?"))
                .header("Location", containsString("code="));
    }

    @DisplayName("Finish authorization missing issuer_state, variant b")
    @Test
    @Requirement({"PIDI-317","PIDI-19"})
    @XrayTest(key = "PIDI-863")
    void test002() {
        new FinishAuthorizationRequestBuilder("/b/finish-authorization")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization empty issuer_state, variant b")
    @Test
    @Requirement({"PIDI-317","PIDI-19"})
    @XrayTest(key = "PIDI-951")
    void test003() {
        new FinishAuthorizationRequestBuilder("/b/finish-authorization")
                .withIssuerState("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization malformed issuer_state, variant b")
    @Test
    @Requirement({"PIDI-317","PIDI-19"})
    @XrayTest(key = "PIDI-936")
    void test004() {
        new FinishAuthorizationRequestBuilder("/b/finish-authorization")
                .withIssuerState("?nval~d")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization invalid issuer_state reference, variant b")
    @Test
    @Requirement({"PIDI-317","PIDI-19"})
    @XrayTest(key = "PIDI-923")
    void test005() {
        var requestUri = steps.doPAR();
        steps.doAuthorize(requestUri);
        new FinishAuthorizationRequestBuilder("/b/finish-authorization")
                .withIssuerState(UUID.randomUUID().toString())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization invalid next expected request, variant b")
    @Test
    void test006() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        steps.doFinishAuthorization(issuerState);
        new FinishAuthorizationRequestBuilder("/b/finish-authorization")
                .withIssuerState(issuerState)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.FOUND)
                .header("location", startsWith("https://secure.redirect.com?"))
                .header("Location", containsString("error=invalid_request"));
    }
}
