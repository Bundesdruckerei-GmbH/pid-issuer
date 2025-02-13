/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.nimbusds.jose.JOSEObjectType;
import de.bdr.openid4vc.vci.data.TokenType;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import de.bdr.pidi.end2end.requests.TokenRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import de.bdr.pidi.testdata.TestConfig;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Date;

import static de.bdr.pidi.end2end.requests.TokenRequestBuilder.getTokenPath;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

class C1TokenTest extends RemoteTest {
    public static final FlowVariant FLOW_VARIANT = FlowVariant.C1;
    private final Steps steps = new Steps(FLOW_VARIANT);

    @Test
    @DisplayName("Token request happy path, variant c1")
    @Requirement({"PIDI-20", "PIDI-282"})
    @XrayTest(key = "PIDI-572")
    void test001() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("token_type", is(TokenType.DPOP.getValue()))
                .body("access_token", is(notNullValue()))
                .body("refresh_token", is(notNullValue()))
                .body("expires_in", isA(Integer.class))
                .body("c_nonce", is(notNullValue()))
                .body("c_nonce", matchesPattern(TestUtils.NONCE_REGEX))
                .body("c_nonce_expires_in", isA(Integer.class))
        ;
    }

    @Test
    @Requirement("PIDI-163")
    @DisplayName("Token request missing code_verifier, variant c1")
    @XrayTest(key = "PIDI-388")
    void test002() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .withoutCodeVerifier()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'code_verifier'"));
    }

    @Test
    @Requirement("PIDI-163")
    @DisplayName("Token request invalid grant, variant c1")
    @XrayTest(key = "PIDI-386")
    void test003() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .withCodeVerifier("ABCDEFGHIJklmnopqrstUVWXYZ-._~0123456789-50Zeichen~")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("Invalid code verifier"));
    }

    @Test
    @DisplayName("Token request empty grant_type, variant c1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-592")
    void test006() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .withGrantType("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'grant_type'"));
    }

    @Test
    @Requirement("PIDI-163")
    @DisplayName("Token request malformed code_verifier, variant c1")
    @XrayTest(key = "PIDI-387")
    void test004() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .withCodeVerifier("\\.[]{}()<>*+-=!?^$|")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid code verifier"));
    }

    @Test
    @DisplayName("Token request missing grant_type, variant c1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-590")
    void test005() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .withoutGrantType()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Missing required parameter 'grant_type'"));
    }

    @Test
    @DisplayName("Token request missing redirect_uri, variant c1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-597")
    void test007() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .withoutRedirectUri()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Invalid redirect URI"));
    }

    @Test
    @DisplayName("Token request malformed redirect_uri, variant c1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-598")
    void test008() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .withRedirectUri("foo")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("Invalid redirect URI"));
    }

    @Test
    @DisplayName("Token request missing authorization code, variant c1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-593")
    void test009() {
        TokenRequestBuilder.valid(FLOW_VARIANT, RandomUtil.randomString())
                .withoutAuthorizationCode()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("invalid authorization code"));
    }

    @Test
    @DisplayName("Token request malformed authorization code, variant c1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-594")
    void test010() {
        TokenRequestBuilder.valid(FLOW_VARIANT, RandomUtil.randomString())
                .withAuthorizationCode("$foo?")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("invalid authorization code"));
    }

    @Test
    @DisplayName("Token request empty authorization code, variant c1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-596")
    void test011() {
        TokenRequestBuilder.valid(FLOW_VARIANT, RandomUtil.randomString())
                .withAuthorizationCode("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("invalid authorization code"));
    }

    @Test
    @DisplayName("Token request missing content type, variant c1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-599")
    void test012() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .withoutContentType()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("Bad request"));
    }

    @Test
    @DisplayName("Token request missing referenced session, variant c1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-600")
    void test013() {
        TokenRequestBuilder.valid(FLOW_VARIANT, RandomUtil.randomString())
                .withAuthorizationCode("not referenced session")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("invalid authorization code"));
    }

    @Test
    @DisplayName("Token request invalid grant type, variant c1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-601")
    void test014() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .withGrantType("invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_grant_type"))
                .body("error_description", is("Grant type \"invalid\" unsupported"));
    }

    @Test
    @DisplayName("Token request invalid redirect_uri, variant c1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-602")
    void test015() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .withRedirectUri("https://example.com")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("Invalid redirect URI"));
    }

    @Test
    @DisplayName("Token request missing dpop header, variant c1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-961")
    void test016() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .withoutDpopHeader()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_dpop_proof"))
                .body("error_description", is("DPoP header not present"));
    }

    @Test
    @DisplayName("Token request empty dpop header, variant c1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-967")
    void test017() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .withHeader("dpop", null)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_dpop_proof"))
                .body("error_description", is("DPoP header not present"));
    }

    @Test
    @DisplayName("Token request, wrong dpop httpMethod, variant c1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-971")
    void test018() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withDpopHeader(FLOW_VARIANT, HttpMethod.GET, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_dpop_proof"))
                .body("error_description", is("htm value mismatch"));
    }

    @Test
    @DisplayName("Token request, wrong dpop path, variant c1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-975")
    void test019() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withDpopHeader(FlowVariant.C, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_dpop_proof"))
                .body("error_description", is("htu value mismatch"));
    }

    @Test
    @DisplayName("Token request, JWT not parsable, variant c1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-978")
    void test020() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withHeader("dpop", "abc")
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_dpop_proof"))
                .body("error_description", is("dpop proof parsing error"));
    }

    @Test
    @DisplayName("Token request, multiple dpop header, variant c1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-983")
    void test021() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        final var dpopProof = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + FLOW_VARIANT.urlPath), faResponse.get("dpop_nonce")).serialize();
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withHeaders("dpop", dpopProof, dpopProof)
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_dpop_proof"))
                .body("error_description", is("Multiple dpop headers in request"));
    }

    @Test
    @DisplayName("Token request, multiple values in dpop header, variant c1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-987")
    void test022() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        final var dpopProof = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + FLOW_VARIANT.urlPath), faResponse.get("dpop_nonce")).serialize();
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withHeader("dpop", dpopProof + ", " + dpopProof)
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_dpop_proof"))
                .body("error_description", is("Multiple values in dpop header"));
    }

    @Test
    @DisplayName("Token request, dpop jwt fictional / too young, variant c1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-990")
    void test023() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        Date iat = java.sql.Timestamp.valueOf(LocalDateTime.now().plusDays(1));
        final var dpopProof = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + getTokenPath(FLOW_VARIANT)), faResponse.get("dpop_nonce"), iat).serialize();
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withHeader("dpop", dpopProof)
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_dpop_proof"))
                .body("error_description", is("proof too young"));
    }


    @Test
    @DisplayName("Token request, dpop jwt stale / too old, variant c1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-991")
    void test024() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        Date iat = java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(1));
        final var dpopProof = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + getTokenPath(FLOW_VARIANT)), faResponse.get("dpop_nonce"), iat).serialize();
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withHeader("dpop", dpopProof)
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_dpop_proof"))
                .body("error_description", is("proof too old"));
    }

    @Test
    @DisplayName("Token request, invalid signature, variant c1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1004")
    void test025() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);

        final var dpopProof = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + getTokenPath(FLOW_VARIANT)), faResponse.get("dpop_nonce")).serialize()
                // signature is the last part, so adding something makes it invalid
                + "aaaa";

        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withHeader("dpop", dpopProof)
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_dpop_proof"))
                .body("error_description", is("Invalid dpop proof: invalid signature"));
    }

    @Test
    @DisplayName("Token request, invalid type, variant c1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1010")
    void test026() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);

        final var dpopProof = TestUtils.getDpopProof(HttpMethod.POST,
                        URI.create(TestConfig.pidiBaseUrl() + getTokenPath(FLOW_VARIANT)),
                        faResponse.get("dpop_nonce"),
                        new JOSEObjectType("dpop+jXt"))
                .serialize();

        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withHeader("dpop", dpopProof)
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_dpop_proof"))
                .body("error_description", is("Invalid dpop proof: invalid typ"));
    }

    @Test
    @DisplayName("Token request, nonce missing, variant c1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1019")
    void test027() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);

        final var dpopProof = TestUtils.getDpopProof(HttpMethod.POST,
                        URI.create(TestConfig.pidiBaseUrl() + getTokenPath(FLOW_VARIANT)),
                        null)
                .serialize();

        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withHeader("dpop", dpopProof)
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("use_dpop_nonce"))
                .body("error_description", is("nonce value missing"));
    }

    @Test
    @DisplayName("Token request key mismatch, variant c1")
    @Requirement("PIDI-346")
    @XrayTest(key = "PIDI-1490")
    @Disabled("PIDI-1688: Temporarily disable Client Attestation, except in B'")
    void test028() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withDpopHeader(FLOW_VARIANT, TestUtils.DIFFERENT_KEY_PAIR, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("Key mismatch"))
        ;
    }

    @Test
    @DisplayName("Token request session expired, variant c1")
    @Requirement({"PIDI-266"})
    @SuppressWarnings("java:S2925")
    @XrayTest(key = "PIDI-2324")
    void test029() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("Session is expired"))
        ;
    }
}
