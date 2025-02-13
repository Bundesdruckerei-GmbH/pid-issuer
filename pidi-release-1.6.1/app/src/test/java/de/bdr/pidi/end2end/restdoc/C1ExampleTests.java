/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.restdoc;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import de.bdr.openid4vc.vci.data.TokenType;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.AuthorizationRequestBuilder;
import de.bdr.pidi.end2end.requests.CredentialRequestBuilder;
import de.bdr.pidi.end2end.requests.Documentation;
import de.bdr.pidi.end2end.requests.FinishAuthorizationRequestBuilder;
import de.bdr.pidi.end2end.requests.PushedAuthorizationRequestBuilder;
import de.bdr.pidi.end2end.requests.RefreshTokenRequestBuilder;
import de.bdr.pidi.end2end.requests.TokenRequestBuilder;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;

@Tag("e2e")
class C1ExampleTests extends RestDocTest {
    private static final String LOCATION = "Location";

    @Override
    FlowVariant flowVariant() {
        return FlowVariant.C1;
    }

    @Test
    @DisplayName("PAR happy path, variant c1")
    void test003() {
        var clientId = ClientIds.validClientId().toString();
        PushedAuthorizationRequestBuilder.valid(flowVariant(), clientId)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.CREATED)
                .body("expires_in", is(60))
                .body("request_uri", startsWith("urn:ietf:params:oauth:request_uri:"))
                .body("request_uri", hasLength(56));
    }

    @Test
    @DisplayName("Authorize happy path, variant c1")
    void test004() {
        var clientId = ClientIds.validClientId().toString();
        String requestUri = steps.doPAR(clientId);
        AuthorizationRequestBuilder.valid(flowVariant(), clientId, requestUri)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.SEE_OTHER)
                .header(LOCATION, containsString("?SAMLRequest="));
    }

    @Test
    @DisplayName("Finish authorization happy path, variant c1")
    void test005() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(clientId, requestUri);
        FinishAuthorizationRequestBuilder.valid(flowVariant(), issuerState)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.FOUND)
                .header(LOCATION, startsWith("https://secure.redirect.com?"))
                .header(LOCATION, containsString("code="));
    }

    @Test
    @DisplayName("Credential request happy path with SdJwt, variant c1")
    @Requirement("PIDI-232")
    void test006() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId, new Documentation("sdjwt/par"));
        var issuerState = steps.doAuthorize(clientId, requestUri, new Documentation("sdjwt/authorize"));
        var faResponse = steps.doFinishAuthorization(issuerState, new Documentation("sdjwt/finish-authorization"));
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"), new Documentation("sdjwt/token"));
        CredentialRequestBuilder
                .validSdJwt(flowVariant(), tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(flowVariant(), clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .withDocumentation(steps.prefixWithVariant(new Documentation("sdjwt/credential")))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credential", is(notNullValue()));
    }

    @Test
    @DisplayName("Credential request happy path with mdoc, variant c1")
    @Requirement("PIDI-421")
    void test007() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId, new Documentation("mdoc/par"));
        var issuerState = steps.doAuthorize(clientId, requestUri, new Documentation("mdoc/authorize"));
        var faResponse = steps.doFinishAuthorization(issuerState, new Documentation("mdoc/finish-authorization"));
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"), new Documentation("mdoc/token"));
        CredentialRequestBuilder
                .validMdoc(flowVariant(), tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(flowVariant(), clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .withDocumentation(steps.prefixWithVariant(new Documentation("mdoc/credential")))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credential", is(notNullValue()));
    }

    @Test
    @DisplayName("Credential batch request happy path with SdJwt, variant c1")
    void test008() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId, new Documentation("sdjwt-batch/par"));
        var issuerState = steps.doAuthorize(clientId, requestUri, new Documentation("sdjwt-batch/authorize"));
        var faResponse = steps.doFinishAuthorization(issuerState, new Documentation("sdjwt-batch/finish-authorization"));
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"), new Documentation("sdjwt-batch/token"));
        CredentialRequestBuilder
                .validSdJwt(flowVariant(), tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProofs(flowVariant(), clientId, Instant.now(), tokenResponse.get("c_nonce"), 2)
                .withDocumentation(steps.prefixWithVariant(new Documentation("sdjwt-batch/credential")))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credentials", hasSize(2));
    }

    @Test
    @DisplayName("Credential batch request happy path with mdoc, variant c1")
    void test009() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId, new Documentation("mdoc-batch/par"));
        var issuerState = steps.doAuthorize(clientId, requestUri, new Documentation("mdoc-batch/authorize"));
        var faResponse = steps.doFinishAuthorization(issuerState, new Documentation("mdoc-batch/finish-authorization"));
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"), new Documentation("mdoc-batch/token"));
        CredentialRequestBuilder
                .validMdoc(flowVariant(), tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProofs(flowVariant(), clientId, Instant.now(), tokenResponse.get("c_nonce"), 2)
                .withDocumentation(steps.prefixWithVariant(new Documentation("mdoc-batch/credential")))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credentials", hasSize(2));
    }

    @Test
    @DisplayName("Token refresh happy path, variant c1")
    void test010() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(clientId, requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"), null);
        String refreshToken = tokenResponse.get("refresh_token");
        String newDpopNonce = steps.doRefreshTokenInitRequest(clientId, refreshToken, null);

        RefreshTokenRequestBuilder.valid(flowVariant(), clientId, newDpopNonce, refreshToken)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("token_type", is(TokenType.DPOP.getValue()))
                .body("access_token", is(notNullValue()))
                .body("expires_in", isA(Integer.class))
                .header("DPoP-Nonce", is(notNullValue()))
                .header("DPoP-Nonce", not(newDpopNonce));
    }

    @Test
    @DisplayName("Credential request via refresh token happy path with sdjwt, variant c1")
    void test014() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId, new Documentation("sdjwt-refresh/par"));
        var issuerState = steps.doAuthorize(clientId, requestUri, new Documentation("sdjwt-refresh/authorize"));
        var faResponse = steps.doFinishAuthorization(issuerState, new Documentation("sdjwt-refresh/finish-authorization"));
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"), new Documentation("sdjwt-refresh/token"));
        var refreshToken = tokenResponse.get("refresh_token");
        var refreshDpopNonce = steps.doRefreshTokenInitRequest(clientId, refreshToken, new Documentation("sdjwt-refresh/token-refresh-init"));
        var refreshResponse = steps.doRefreshTokenRequest(clientId, refreshToken, refreshDpopNonce, new Documentation("sdjwt-refresh/token-refresh"));
        CredentialRequestBuilder
                .validSdJwt(flowVariant(), refreshResponse.get("dpop_nonce"), refreshResponse.get("access_token"))
                .withProof(flowVariant(), clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), refreshResponse.get("c_nonce"))
                .withDocumentation(steps.prefixWithVariant(new Documentation("sdjwt-refresh/credential")))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .header("Content-Type", is("application/json"))
                .body("credential", is(notNullValue()));
    }

    @Test
    @DisplayName("Credential request via refresh token happy path with mdoc, variant c1")
    void test015() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId, new Documentation("mdoc-refresh/par"));
        var issuerState = steps.doAuthorize(clientId, requestUri, new Documentation("mdoc-refresh/authorize"));
        var faResponse = steps.doFinishAuthorization(issuerState, new Documentation("mdoc-refresh/finish-authorization"));
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"), new Documentation("mdoc-refresh/token"));
        var refreshToken = tokenResponse.get("refresh_token");
        var refreshDpopNonce = steps.doRefreshTokenInitRequest(clientId, refreshToken, new Documentation("mdoc-refresh/token-refresh-init"));
        var refreshResponse = steps.doRefreshTokenRequest(clientId, refreshToken, refreshDpopNonce, new Documentation("mdoc-refresh/token-refresh"));
        CredentialRequestBuilder
                .validMdoc(flowVariant(), refreshResponse.get("dpop_nonce"), refreshResponse.get("access_token"))
                .withProof(flowVariant(), clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), refreshResponse.get("c_nonce"))
                .withDocumentation(steps.prefixWithVariant(new Documentation("mdoc-refresh/credential")))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .header("Content-Type", is("application/json"))
                .body("credential", is(notNullValue()));
    }

    @Test
    @DisplayName("Token request, invalid grant-type, variant c1")
    void test011() {
        TokenRequestBuilder.valid(flowVariant(), "dpopNonce")
                .withGrantType("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_grant_type"))
                .body("error_description", is("Grant type \"invalid\" unsupported"));
    }

    @Test
    @DisplayName("Token request, empty grant-type, variant c1")
    void test012() {
        TokenRequestBuilder.valid(flowVariant(), "dpopNonce")
                .withGrantType("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'grant_type'"));
    }

    @Test
    @DisplayName("Token request, missing grant-type, variant c1")
    void test013() {
        TokenRequestBuilder.valid(flowVariant(), "dpopNonce")
                .withoutGrantType()

                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'grant_type'"));
    }
}
