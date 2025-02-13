/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.restdoc;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.AuthorizationRequestBuilder;
import de.bdr.pidi.end2end.requests.CredentialRequestBuilder;
import de.bdr.pidi.end2end.requests.Documentation;
import de.bdr.pidi.end2end.requests.FinishAuthorizationRequestBuilder;
import de.bdr.pidi.end2end.requests.PushedAuthorizationRequestBuilder;
import de.bdr.pidi.end2end.requests.TokenRequestBuilder;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static de.bdr.pidi.testdata.ValidTestData.REDIRECT_URI;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;

@Tag("e2e")
class BExampleTests extends RestDocTest {
    private static final String REQUEST_URI = "request_uri";
    private static final String LOCATION = "Location";

    @Override
    FlowVariant flowVariant() {
        return FlowVariant.B;
    }

    @Test
    @DisplayName("Authorize happy path, variant b")
    void test001() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        AuthorizationRequestBuilder.valid(flowVariant(), clientId, requestUri)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.SEE_OTHER)
                .header(LOCATION, containsString("?SAMLRequest="));
    }

    @Test
    @DisplayName("PAR happy path, variant b")
    void test002() {
        var clientId = ClientIds.validClientId().toString();
        PushedAuthorizationRequestBuilder.valid(flowVariant(), clientId)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.CREATED)
                .body("expires_in", is(60))
                .body(REQUEST_URI, startsWith("urn:ietf:params:oauth:request_uri:"))
                .body(REQUEST_URI, hasLength(56));
    }

    @Test
    @DisplayName("Finish authorization happy path, variant b")
    void test003() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(clientId, requestUri);
        FinishAuthorizationRequestBuilder.valid(flowVariant(), issuerState)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.FOUND)
                .header(LOCATION, startsWith(REDIRECT_URI))
                .header(LOCATION, containsString("code="));
    }

    @Test
    @DisplayName("Credential request happy path with SdJwt, variant b")
    @Requirement("PIDI-727")
    void test004() {
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
    @DisplayName("Credential request happy path with mdoc, variant b")
    @Requirement("PIDI-727")
    void test005() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId, new Documentation("mdoc/par"));
        var issuerState = steps.doAuthorize(clientId, requestUri, new Documentation("mdoc/authorize"));
        var faResponse = steps.doFinishAuthorization(issuerState, new Documentation("mdoc/finish-authorization"));
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"), new Documentation("mdoc/token"));
        CredentialRequestBuilder
                .validMdoc(flowVariant(), tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withDocumentation(steps.prefixWithVariant(new Documentation("mdoc/credential")))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credential", is(notNullValue()));
    }

    @Test
    @DisplayName("Credential batch request with SdJwt is invalid, variant b")
    void test006() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(clientId, requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"), null);
        CredentialRequestBuilder
                .validSdJwt(flowVariant(), tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProofs(flowVariant(), clientId, Instant.now(), tokenResponse.get("c_nonce"), 2)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("No proofs expected"));
    }

    @Test
    @DisplayName("Credential request with mdoc and proof is invalid, variant b")
    void test007() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(clientId, requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"), null);
        CredentialRequestBuilder
                .validMdoc(flowVariant(), tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(flowVariant(), clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("Neither proof nor proofs expected"));
    }

    @Test
    @DisplayName("Token request, invalid grant-type, variant b")
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
    @DisplayName("Token request, empty grant-type, variant b")
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
    @DisplayName("Token request, mssing grant-type, variant b")
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
