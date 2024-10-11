/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.FinishAuthorizationRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;

class B1FinishAuthorizationTest extends RemoteTest {
    private final Steps steps = new Steps(FlowVariant.B1);

    @DisplayName("Finish authorization happy path, variant b1")
    @Test
    @Requirement({"PIDI-317", "PIDI-19","PIDI-1688"})
    @XrayTest(key = "PIDI-1182")
    void test001() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        new FinishAuthorizationRequestBuilder("/b1/finish-authorization")
                .withIssuerState(issuerState)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.FOUND)
                .header("location", startsWith("https://secure.redirect.com?"))
                .header("Location", containsString("code="))
                .header("dpop-nonce", is(notNullValue()));
    }

    @DisplayName("Finish authorization missing issuer_state, variant b1")
    @Test
    @Requirement({"PIDI-317", "PIDI-19"})
    @XrayTest(key = "PIDI-1188")
    void test002() {
        new FinishAuthorizationRequestBuilder("/b1/finish-authorization")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization empty issuer_state, variant b1")
    @Test
    @Requirement({"PIDI-317", "PIDI-19"})
    @XrayTest(key = "PIDI-1196")
    void test003() {
        new FinishAuthorizationRequestBuilder("/b1/finish-authorization")
                .withIssuerState("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization malformed issuer_state, variant b1")
    @Test
    @Requirement({"PIDI-317", "PIDI-19"})
    @XrayTest(key = "PIDI-1274")
    void test004() {
        new FinishAuthorizationRequestBuilder("/b1/finish-authorization")
                .withIssuerState("?nval~d")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization invalid issuer_state reference, variant b1")
    @Test
    @Requirement({"PIDI-317", "PIDI-19"})
    @XrayTest(key = "PIDI-1284")
    void test005() {
        var requestUri = steps.doPAR();
        steps.doAuthorize(requestUri);
        new FinishAuthorizationRequestBuilder("/b1/finish-authorization")
                .withIssuerState(UUID.randomUUID().toString())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization invalid next expected request, variant b1")
    @Test
    void test015() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        steps.doFinishAuthorization(issuerState);
        new FinishAuthorizationRequestBuilder("/b1/finish-authorization")
                .withIssuerState(issuerState)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.FOUND)
                .header("location", startsWith("https://secure.redirect.com?"))
                .header("Location", containsString("error=invalid_request"));
    }
}
