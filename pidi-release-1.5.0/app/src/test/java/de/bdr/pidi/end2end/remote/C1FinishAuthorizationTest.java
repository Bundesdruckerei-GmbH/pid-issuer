/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.FinishAuthorizationRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;

@Slf4j
class C1FinishAuthorizationTest extends RemoteTest {

    private final Steps steps = new Steps(FlowVariant.C1);

    @DisplayName("Finish authorization happy path, variant c1")
    @Test
    @Requirement({"PIDI-317", "PIDI-19"})
    @XrayTest(key = "PIDI-284")
    void test001() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        new FinishAuthorizationRequestBuilder("/c1/finish-authorization")
                .withIssuerState(issuerState)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.FOUND)
                .header("location", startsWith("https://secure.redirect.com?"))
                .header("Location", containsString("code="))
                .header("dpop-nonce", is(notNullValue()));
    }

    @DisplayName("Finish authorization missing issuer_state, variant c1")
    @Test
    @Requirement({"PIDI-317", "PIDI-19"})
    @XrayTest(key = "PIDI-378")
    void test002() {
        new FinishAuthorizationRequestBuilder("/c1/finish-authorization")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization empty issuer_state, variant c1")
    @Test
    @Requirement({"PIDI-317", "PIDI-19"})
    @XrayTest(key = "PIDI-382")
    void test003() {
        new FinishAuthorizationRequestBuilder("/c1/finish-authorization")
                .withIssuerState("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization malformed issuer_state, variant c1")
    @Test
    @Requirement({"PIDI-317", "PIDI-19"})
    @XrayTest(key = "PIDI-384")
    void test004() {
        new FinishAuthorizationRequestBuilder("/c1/finish-authorization")
                .withIssuerState("?nval~d")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization invalid issuer_state reference, variant c1")
    @Test
    @Requirement({"PIDI-317", "PIDI-19"})
    @XrayTest(key = "PIDI-426")
    void test005() {
        var requestUri = steps.doPAR();
        steps.doAuthorize(requestUri);
        new FinishAuthorizationRequestBuilder("/c1/finish-authorization")
                .withIssuerState(UUID.randomUUID().toString())
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("invalid issuer_state"));
    }

    @DisplayName("Finish authorization invalid next expected request, variant c1")
    @Test
    void test006() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        steps.doFinishAuthorization(issuerState);
        new FinishAuthorizationRequestBuilder("/c1/finish-authorization")
                .withIssuerState(issuerState)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.FOUND)
                .header("location", startsWith("https://secure.redirect.com?"))
                .header("Location", containsString("error=invalid_request"));
    }


}
