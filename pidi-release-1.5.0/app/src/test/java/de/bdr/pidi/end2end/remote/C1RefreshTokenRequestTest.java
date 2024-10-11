/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import de.bdr.openid4vc.vci.data.TokenType;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.CredentialRequestBuilder;
import de.bdr.pidi.end2end.requests.RefreshTokenRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

class C1RefreshTokenRequestTest extends RemoteTest {
    private final Steps steps = new Steps(FlowVariant.C1);

    @Test
    @DisplayName("Token refresh happy path, variant c1")
    @Requirement({"PIDI-623","PIDI-346","PIDI-1688"})
    @XrayTest(key = "PIDI-1037")
    void test001() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        String refreshToken = tokenResponse.get("refresh_token");
        var response = RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, faResponse.get("dpop_nonce"), tokenResponse.get("refresh_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("token_type", is(nullValue()))
                .body("access_token", is(nullValue()))
                .header("DPoP-Nonce", is(notNullValue()));
        String newDpopNonce = response.extract().header("DPoP-Nonce");

        RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, newDpopNonce, refreshToken)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("token_type", is(TokenType.DPOP.getValue()))
                .body("access_token", is(notNullValue()))
                .body("expires_in", isA(Integer.class))
                .header("DPoP-Nonce", is(notNullValue()))
                .header("DPoP-Nonce", not(newDpopNonce))
        ;
    }

    @Test
    @DisplayName("Token refresh invalid grant type, variant c1")
    @Requirement("PIDI-623")
    @XrayTest(key = "PIDI-1042")
    void test002() {
        var clientId = ClientIds.validClientId().toString();
        String refreshToken = "invalid" + UUID.randomUUID();
        var response = RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, null, refreshToken)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("token_type", is(nullValue()))
                .body("access_token", is(nullValue()))
                .header("DPoP-Nonce", is(notNullValue()));
        String newDpopNonce = response.extract().header("DPoP-Nonce");

        RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, newDpopNonce, refreshToken).withGrantType("foo")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_grant_type"))
                .body("error_description", is("Grant type \"foo\" unsupported"));

    }

    @Test
    @DisplayName("Token refresh empty grant type, variant c1")
    @Requirement("PIDI-623")
    @XrayTest(key = "PIDI-1051")
    void test003() {
        var clientId = ClientIds.validClientId().toString();
        String refreshToken = "invalid" + UUID.randomUUID();
        var response = RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, null, refreshToken)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("token_type", is(nullValue()))
                .body("access_token", is(nullValue()))
                .header("DPoP-Nonce", is(notNullValue()));
        String newDpopNonce = response.extract().header("DPoP-Nonce");

        RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, newDpopNonce, refreshToken).withGrantType("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'grant_type'"));
    }

    @Test
    @DisplayName("Token refresh missing grant type, variant c1")
    @Requirement("PIDI-623")
    @XrayTest(key = "PIDI-1045")
    void test004() {
        var clientId = ClientIds.validClientId().toString();
        String refreshToken = "invalid" + UUID.randomUUID();
        var response = RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, null, refreshToken)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("token_type", is(nullValue()))
                .body("access_token", is(nullValue()))
                .header("DPoP-Nonce", is(notNullValue()));
        String newDpopNonce = response.extract().header("DPoP-Nonce");

        RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, newDpopNonce, refreshToken).withoutGrantType()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'grant_type'"));

    }

    @Test
    @DisplayName("Token refresh invalid jwt, variant c1")
    @Requirement("PIDI-623")
    @XrayTest(key = "PIDI-1044")
    void test005() {
        var clientId = ClientIds.validClientId().toString();
        String refreshToken = "invalid" + UUID.randomUUID();
        var response = RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, null, refreshToken)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("token_type", is(nullValue()))
                .body("access_token", is(nullValue()))
                .header("DPoP-Nonce", is(notNullValue()));
        String newDpopNonce = response.extract().header("DPoP-Nonce");

        RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, newDpopNonce, refreshToken)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("Refresh token invalid"));

    }

    @Test
    @DisplayName("Token refresh invalid refresh_token, variant c1")
    @Requirement("PIDI-623")
    @XrayTest(key = "PIDI-1043")
    void test006() {
        var clientId = ClientIds.validClientId().toString();
        String refreshToken = "invalid" + UUID.randomUUID();
        var response = RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, null, refreshToken)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("token_type", is(nullValue()))
                .body("access_token", is(nullValue()))
                .header("DPoP-Nonce", is(notNullValue()));
        String newDpopNonce = response.extract().header("DPoP-Nonce");

        RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, newDpopNonce, "")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Refresh token missing"));

    }

    @Test
    @DisplayName("Token refresh missing refresh_token, variant c1")
    @Requirement("PIDI-623")
    @XrayTest(key = "PIDI-1046")
    void test007() {
        var clientId = ClientIds.validClientId().toString();
        String refreshToken = "invalid" + UUID.randomUUID();
        var response = RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, null, refreshToken)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("token_type", is(nullValue()))
                .body("access_token", is(nullValue()))
                .header("DPoP-Nonce", is(notNullValue()));
        String newDpopNonce = response.extract().header("DPoP-Nonce");

        RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, newDpopNonce, refreshToken).withoutRefreshToken()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Refresh token missing"));

    }

    @Test
    @DisplayName("Token refresh successfully credential request call after refresh, variant c1")
    @Requirement("PIDI-623")
    @XrayTest(key = "PIDI-1377")
    void test008() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        String refreshToken = tokenResponse.get("refresh_token");
        var response = RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, faResponse.get("dpop_nonce"), tokenResponse.get("refresh_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("token_type", is(nullValue()))
                .body("access_token", is(nullValue()))
                .header("DPoP-Nonce", is(notNullValue()));
        String newDpopNonce = response.extract().header("DPoP-Nonce");

        var newRefreshResponse = RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, newDpopNonce, refreshToken)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("token_type", is(TokenType.DPOP.getValue()))
                .body("access_token", is(notNullValue()))
                .body("expires_in", isA(Integer.class))
                .header("DPoP-Nonce", is(notNullValue()))
                .header("DPoP-Nonce", not(newDpopNonce));
        var jsonPath = newRefreshResponse.extract().body().jsonPath();
        var headers = newRefreshResponse.extract().headers();
        var values = new HashMap<String, String>();
        values.put("access_token", jsonPath.getString("access_token"));
        values.put("c_nonce", jsonPath.getString("c_nonce"));
        values.put("dpop_nonce", headers.getValue("DPoP-Nonce"));
        values.put("refresh_token", refreshToken);
        CredentialRequestBuilder
                .validSdJwt(FlowVariant.C1, values.get("dpop_nonce"), values.get("access_token"))
                .withProof(FlowVariant.C1, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), values.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK).header("Content-Type", is("application/json"))
                .body("credential", is(notNullValue()))
                .body("c_nonce", is(notNullValue()))
                .body("c_nonce", matchesPattern(TestUtils.NONCE_REGEX))
                .body("c_nonce_expires_in", isA(Integer.class));
    }
    @Test
    @DisplayName("Token refresh key mismatch, variant c1")
    @Requirement("PIDI-346")
    @XrayTest(key = "PIDI-1506")
    void test009() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        String refreshToken = tokenResponse.get("refresh_token");
        var response = RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, faResponse.get("dpop_nonce"), tokenResponse.get("refresh_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("token_type", is(nullValue()))
                .body("access_token", is(nullValue()))
                .header("DPoP-Nonce", is(notNullValue()));
        String newDpopNonce = response.extract().header("DPoP-Nonce");

        RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, newDpopNonce, refreshToken)
                .withDpopHeader(FlowVariant.C1, TestUtils.DIFFERENT_KEY_PAIR, newDpopNonce)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("Key mismatch"))
        ;
    }
    @Test
    @DisplayName("Token refresh happy path with optional scope, variant c1")
    @Requirement({"PIDI-771","PIDI-1688"})
    @XrayTest(key = "PIDI-1066")
    void test010() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        String refreshToken = tokenResponse.get("refresh_token");
        var response = RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, faResponse.get("dpop_nonce"), tokenResponse.get("refresh_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("token_type", is(nullValue()))
                .body("access_token", is(nullValue()))
                .header("DPoP-Nonce", is(notNullValue()));
        String newDpopNonce = response.extract().header("DPoP-Nonce");

        RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, newDpopNonce, refreshToken)
                .withOptionalScope("pid")
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
    @DisplayName("Token refresh optional scope is invalid, variant c1")
    @Requirement("PIDI-771")
    @XrayTest(key = "PIDI-1067")
    void test011() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        String refreshToken = tokenResponse.get("refresh_token");
        var response = RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, faResponse.get("dpop_nonce"), tokenResponse.get("refresh_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("token_type", is(nullValue()))
                .body("access_token", is(nullValue()))
                .header("DPoP-Nonce", is(notNullValue()));
        String newDpopNonce = response.extract().header("DPoP-Nonce");

        RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, newDpopNonce, refreshToken)
                .withOptionalScope("invalidScope")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_scope"))
                .body("error_description", is("Scopes invalidScope not granted"));
    }
    @Test
    @DisplayName("Token refresh with empty scope, variant c1")
    @Requirement("PIDI-771")
    @XrayTest(key = "PIDI-1790")
    void test012() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        String refreshToken = tokenResponse.get("refresh_token");
        var response = RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, faResponse.get("dpop_nonce"), tokenResponse.get("refresh_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("token_type", is(nullValue()))
                .body("access_token", is(nullValue()))
                .header("DPoP-Nonce", is(notNullValue()));
        String newDpopNonce = response.extract().header("DPoP-Nonce");

        RefreshTokenRequestBuilder.valid(FlowVariant.C1, clientId, newDpopNonce, refreshToken)
                .withOptionalScope("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid scope"));
    }
}
