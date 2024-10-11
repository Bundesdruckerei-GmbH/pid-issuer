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

class B1TokenTest extends RemoteTest {
    public static final FlowVariant FLOW_VARIANT = FlowVariant.B1;
    private final Steps steps = new Steps(FLOW_VARIANT);
    @Test
    @DisplayName("Token request happy path, variant b1")
    @Requirement({"PIDI-20","PIDI-282"})
    @XrayTest(key = "PIDI-1229")
    void test001() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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

    @Test
    @DisplayName("Token request missing code_verifier, variant b1")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-1301")
    void test002() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request invalid grant, variant b1")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-1291")
    void test003() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request empty grant_type, variant b1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1194")
    void test006() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request malformed code_verifier, variant b1")
    @Requirement("PIDI-163")
    @XrayTest(key = "PIDI-1279")
    void test004() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request missing grant_type, variant b1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1269")
    void test005() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request missing redirect_uri, variant b1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1186")
    void test007() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request malformed redirect_uri, variant b1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1181")
    void test008() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request missing authorization code, variant b1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1176")
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
    @DisplayName("Token request malformed authorization code, variant b1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1265")
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
    @DisplayName("Token request empty authorization code, variant b1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1255")
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
    @DisplayName("Token request missing content type, variant b1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1243")
    void test012() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .withoutContentType()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("Bad request"));
    }

    @Test
    @DisplayName("Token request missing referenced session, variant b1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1234")
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
    @DisplayName("Token request invalid grant type, variant b1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1307")
    void test014() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request invalid redirect_uri, variant b1")
    @Requirement("PIDI-20")
    @XrayTest(key = "PIDI-1296")
    void test015() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request missing dpop header, variant b1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1287")
    void test016() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request empty dpop header, variant b1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1275")
    void test017() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request, wrong dpop httpMethod, variant b1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1198")
    void test018() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request, wrong dpop path, variant b1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1190")
    void test019() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
                .withDpopHeader(FlowVariant.C, deviceKeyPair, faResponse.get("dpop_nonce"))
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_dpop_proof"))
                .body("error_description", is("htu value mismatch"));
    }

    @Test
    @DisplayName("Token request, JWT not parsable, variant b1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1202")
    void test020() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request, multiple dpop header, variant b1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1260")
    void test021() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        final var dpopProof = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + FLOW_VARIANT.urlPath), faResponse.get("dpop_nonce")).serialize();
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request, multiple values in dpop header, variant b1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1249")
    void test022() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        final var dpopProof = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + FLOW_VARIANT.urlPath), faResponse.get("dpop_nonce")).serialize();
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
                .withHeader("dpop", dpopProof +", "+ dpopProof)
                .withAuthorizationCode(faResponse.get("code"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_dpop_proof"))
                .body("error_description", is("Multiple values in dpop header"));
    }

    @Test
    @DisplayName("Token request, dpop jwt fictional / too young, variant b1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1238")
    void test023() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        Date iat = java.sql.Timestamp.valueOf(LocalDateTime.now().plusDays(1));
        final var dpopProof = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + getTokenPath(FLOW_VARIANT)), faResponse.get("dpop_nonce"), iat).serialize();
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request, dpop jwt stale / too old, variant b1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1228")
    void test024() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        Date iat = java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(1));
        final var dpopProof = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + getTokenPath(FLOW_VARIANT)), faResponse.get("dpop_nonce"), iat).serialize();
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request, invalid signature, variant b1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1302")
    void test025() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);

        final var dpopProof = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + getTokenPath(FLOW_VARIANT)), faResponse.get("dpop_nonce")).serialize()
                // signature is the last part, so adding something makes it invalid
                + "aaaa";

        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request, invalid type, variant b1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1290")
    void test026() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);

        final var dpopProof = TestUtils.getDpopProof(HttpMethod.POST,
                        URI.create(TestConfig.pidiBaseUrl() + getTokenPath(FLOW_VARIANT)),
                        faResponse.get("dpop_nonce"),
                        new JOSEObjectType("dpop+jXt"))
                .serialize();

        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request, nonce missing, variant b1")
    @Requirement("PIDI-227")
    @XrayTest(key = "PIDI-1280")
    void test027() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);

        final var dpopProof = TestUtils.getDpopProof(HttpMethod.POST,
                        URI.create(TestConfig.pidiBaseUrl() + getTokenPath(FLOW_VARIANT)),
                        null)
                .serialize();

        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
    @DisplayName("Token request key mismatch, variant b1")
    @Requirement("PIDI-346")
    @XrayTest(key = "PIDI-1489")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test0028() {
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        TokenRequestBuilder.valid(FLOW_VARIANT, deviceKeyPair, faResponse.get("dpop_nonce"))
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
}
