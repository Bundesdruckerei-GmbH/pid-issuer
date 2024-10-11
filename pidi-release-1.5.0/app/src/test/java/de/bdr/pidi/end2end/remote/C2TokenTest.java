/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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

class C2TokenTest extends RemoteTest {

    public static final FlowVariant FLOW_VARIANT = FlowVariant.C2;
    private final Steps steps = new Steps(FLOW_VARIANT);

    @Test
    @DisplayName("Token request happy path, variant c2")
    @Requirement({"PIDI-20", "PIDI-282"})
    @XrayTest(key = "PIDI-1608")
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
                .body("expires_in", isA(Integer.class))
                .body("c_nonce", is(notNullValue()))
                .body("c_nonce", matchesPattern(TestUtils.NONCE_REGEX))
                .body("c_nonce_expires_in", isA(Integer.class))
        ;
    }

    @DisplayName("Token request missing code_verifier, variant c2")
    @Test
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-1618")
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

    @DisplayName("Token request malformed code_verifier, variant c2")
    @Test
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-1625")
    void test003() {
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

    @DisplayName("Token request invalid grant, variant c2")
    @Test
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-1632")
    void test004() {
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

    @DisplayName("Token request empty grant_type, variant c2")
    @Test
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1638")
    void test005() {
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

    @DisplayName("Token request missing redirect_uri, variant c2")
    @Test
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1646")
    void test006() {
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

    @DisplayName("Token request malformed redirect_uri, variant c2")
    @Test
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1523")
    void test007() {
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

    @DisplayName("Token request missing authorization code, variant c2")
    @Test
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1530")
    void test008() {
        TokenRequestBuilder.valid(FLOW_VARIANT, RandomUtil.randomString())
                .withoutAuthorizationCode()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("invalid authorization code"));
    }

    @DisplayName("Token request malformed authorization code, variant c2")
    @Test
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1538")
    void test009() {
        TokenRequestBuilder.valid(FLOW_VARIANT, RandomUtil.randomString())
                .withAuthorizationCode("$foo?")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("invalid authorization code"));
    }

    @DisplayName("Token request empty authorization code, variant c2")
    @Test
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1593")
    void test010() {
        TokenRequestBuilder.valid(FLOW_VARIANT, RandomUtil.randomString())
                .withAuthorizationCode("")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("invalid authorization code"));
    }

    @DisplayName("Token request missing content type, variant c2")
    @Test
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1601")
    void test011() {
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

    @DisplayName("Token request missing referenced session, variant c2")
    @Test
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1613")
    void test012() {
        TokenRequestBuilder.valid(FLOW_VARIANT, RandomUtil.randomString())
                .withAuthorizationCode("not referenced session")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_grant"))
                .body("error_description", is("invalid authorization code"));
    }

    @DisplayName("Token request invalid grant type, variant c2")
    @Test
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1621")
    void test013() {
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

    @DisplayName("Token request invalid redirect_uri, variant c2")
    @Test
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1628")
    void test014() {
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

    @DisplayName("Token request missing grant_type, variant c2")
    @Test
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1635")
    void test015() {
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
    @DisplayName("Token request missing dpop header, variant c2")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1643")
    void test016() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FlowVariant.C1, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request empty dpop header, variant c2")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1651")
    void test017() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FlowVariant.C1, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request, wrong dpop httpMethod, variant c2")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1527")
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
    @DisplayName("Token request, wrong dpop path, variant c2")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1533")
    void test019() {
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, faResponse.get("dpop_nonce"))
                .withDpopHeader(FlowVariant.C1, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_dpop_proof"))
                .body("error_description", is("htu value mismatch"));
    }

    @Test
    @DisplayName("Token request, JWT not parsable, variant c2")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1570")
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
    @DisplayName("Token request, multiple dpop header, variant c2")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1579")
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
    @DisplayName("Token request, multiple values in dpop header, variant c2")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1587")
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
    @DisplayName("Token request, dpop jwt fictional / too young, variant c2")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1600")
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
    @DisplayName("Token request, dpop jwt stale / too old, variant c2")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1609")
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
    @DisplayName("Token request, invalid signature, variant c2")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1617")
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
    @DisplayName("Token request, invalid type, variant c2")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1624")
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
    @DisplayName("Token request, nonce missing, variant c2")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1631")
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
    @SuppressWarnings("java:S2925")
    @DisplayName("Token request session expired, variant c2")
    @Requirement({"PIDI-266"})
    @XrayTest(key = "PIDI-2323")
    void test028() {
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
