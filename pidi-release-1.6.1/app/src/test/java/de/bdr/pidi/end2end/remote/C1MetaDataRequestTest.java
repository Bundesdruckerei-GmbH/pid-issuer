/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;
import de.bdr.pidi.end2end.requests.MetadataRequestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static de.bdr.pidi.base.PidDataConst.MDOC_TYPE;
import static de.bdr.pidi.testdata.TestUtils.SD_JWT_VCTYPE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

class C1MetaDataRequestTest extends RemoteTest {
    private static final String BASE_URL_REGEX = "https?://\\w++(?:\\.[\\w\\-]+)*+(?::\\d+)?";

    @DisplayName("Credential metadata endpoint happy path, variant c1")
    @Test
    @Requirement({"PIDI-228", "PIDI-613","PIDI-2766"})
    @XrayTest(key = "PIDI-786")
    void test001() {
        new MetadataRequestBuilder()
                .withUrl("/c1/.well-known/openid-credential-issuer")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credential_endpoint", matchesPattern(BASE_URL_REGEX + "/c1/credential"))
                .body("credential_issuer", matchesPattern(BASE_URL_REGEX + "/c1"))
                .body("batch_credential_issuance.batch_size", is(42))
                .body(not("presentation_signing_endpoint"))
                .body("display.size()", is(2))
                .body("display[0].name", is("Bundesdruckerei GmbH"))
                .body("display[0].locale", is("de-DE"))
                .body("display[1].name", is("Bundesdruckerei GmbH"))
                .body("display[1].locale", is("en-US"))
                .body("credential_configurations_supported.size()", is(2))
                .body("credential_configurations_supported.pid-sd-jwt.scope", is("pid"))
                .body("credential_configurations_supported.pid-sd-jwt.cryptographic_binding_methods_supported.size()", is(1))
                .body("credential_configurations_supported.pid-sd-jwt.cryptographic_binding_methods_supported[0]", is("jwk"))
                .body("credential_configurations_supported.pid-sd-jwt.credential_signing_alg_values_supported.size()", is(1))
                .body("credential_configurations_supported.pid-sd-jwt.credential_signing_alg_values_supported[0]", is("ES256"))
                .body("credential_configurations_supported.pid-sd-jwt.format", is("vc+sd-jwt"))
                .body("credential_configurations_supported.pid-sd-jwt.vct", is(SD_JWT_VCTYPE))
                .body("credential_configurations_supported.pid-sd-jwt.proof_types_supported.jwt.proof_signing_alg_values_supported.size()", is(1))
                .body("credential_configurations_supported.pid-sd-jwt.proof_types_supported.jwt.proof_signing_alg_values_supported[0]", is("ES256"))
                .body("credential_configurations_supported.pid-sd-jwt.proof_types_supported", is(notNullValue()))
                .body("credential_configurations_supported.pid-mso-mdoc.format", is("mso_mdoc"))
                .body("credential_configurations_supported.pid-mso-mdoc.doctype", is(MDOC_TYPE))
                .body("credential_configurations_supported.pid-mso-mdoc.scope", is("pid"))
                .body("credential_configurations_supported.pid-mso-mdoc.cryptographic_binding_methods_supported.size()", is(1))
                .body("credential_configurations_supported.pid-mso-mdoc.cryptographic_binding_methods_supported[0]", is("cose_key"))
                .body("credential_configurations_supported.pid-mso-mdoc.credential_signing_alg_values_supported.size()", is(1))
                .body("credential_configurations_supported.pid-mso-mdoc.credential_signing_alg_values_supported[0]", is("ES256"))
                .body("credential_configurations_supported.pid-mso-mdoc.proof_types_supported.jwt.proof_signing_alg_values_supported.size()", is(1))
                .body("credential_configurations_supported.pid-mso-mdoc.proof_types_supported.jwt.proof_signing_alg_values_supported[0]", is("ES256"));
    }

    @DisplayName("Authorization Server Metadata happy path, variant c1")
    @Test
    @Requirement("PIDI-243")
    @XrayTest(key = "PIDI-788")
    void test002() {
        String[] dpopSigningAlgs = JWSAlgorithm.Family.SIGNATURE.stream().map(Algorithm::toString).toArray(String[]::new);
        new MetadataRequestBuilder()
                .withUrl("/c1/.well-known/oauth-authorization-server")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("issuer", matchesPattern(BASE_URL_REGEX + "/c1"))
                .body("authorization_endpoint", matchesPattern(BASE_URL_REGEX + "/c1/authorize"))
                .body("token_endpoint", matchesPattern(BASE_URL_REGEX + "/c1/token"))
                .body("pushed_authorization_request_endpoint", matchesPattern(BASE_URL_REGEX + "/c1/par"))
                .body("require_pushed_authorization_requests", is(true))
                .body("token_endpoint_auth_methods_supported.size()", is(1))
                .body("token_endpoint_auth_methods_supported[0]", is("none"))
                .body("response_types_supported.size()", is(1))
                .body("response_types_supported[0]", is("code"))
                .body("code_challenge_methods_supported.size()", is(1))
                .body("code_challenge_methods_supported[0]", is("S256"))
                .body("dpop_signing_alg_values_supported", contains(dpopSigningAlgs));
    }

    @DisplayName("Authorization Server Metadata alternative happy path, variant c1")
    @Test
    @Requirement("PIDI-400")
    @XrayTest(key = "PIDI-790")
    void test003() {
        String[] dpopSigningAlgs = JWSAlgorithm.Family.SIGNATURE.stream().map(Algorithm::toString).toArray(String[]::new);
        new MetadataRequestBuilder()
                .withUrl("/.well-known/oauth-authorization-server/c1")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("issuer", matchesPattern(BASE_URL_REGEX + "/c1"))
                .body("authorization_endpoint", matchesPattern(BASE_URL_REGEX + "/c1/authorize"))
                .body("token_endpoint", matchesPattern(BASE_URL_REGEX + "/c1/token"))
                .body("pushed_authorization_request_endpoint", matchesPattern(BASE_URL_REGEX + "/c1/par"))
                .body("require_pushed_authorization_requests", is(true))
                .body("token_endpoint_auth_methods_supported.size()", is(1))
                .body("token_endpoint_auth_methods_supported[0]", is("none"))
                .body("response_types_supported.size()", is(1))
                .body("response_types_supported[0]", is("code"))
                .body("code_challenge_methods_supported.size()", is(1))
                .body("code_challenge_methods_supported[0]", is("S256"))
                .body("dpop_signing_alg_values_supported", contains(dpopSigningAlgs));
    }

    @DisplayName("Credential metadata endpoint unknow flow variant, variant c1")
    @Test
    @Requirement("PIDI-228")
    @XrayTest(key = "PIDI-792")
    void test004() {
        new MetadataRequestBuilder()
                .withUrl("/invalid-variant/.well-known/openid-credential-issuer")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("'invalid-variant' is not a known flow variant path"));
    }

    @DisplayName("Authorization Server Metadata invalid flow variant, variant c1")
    @Test
    @Requirement("PIDI-243")
    @XrayTest(key = "PIDI-795")
    void test005() {
        new MetadataRequestBuilder()
                .withUrl("/invalid-variant/.well-known/oauth-authorization-server")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("'invalid-variant' is not a known flow variant path"));
    }

    @DisplayName("Authorization Server Metadata alternative invalid flow variant, variant c1")
    @Test
    @Requirement("PIDI-400")
    @XrayTest(key = "PIDI-796")
    void test006() {
        new MetadataRequestBuilder()
                .withUrl("/.well-known/oauth-authorization-server/invalid-variant")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("'invalid-variant' is not a known flow variant path"));
    }

    @DisplayName("sd jwt metadata happy path,variant c1")
    @Test
    @Requirement("PIDI-401")
    @XrayTest(key = "PIDI-1167")
    void test007() {
        new MetadataRequestBuilder()
                .withUrl("/.well-known/jwt-vc-issuer/c1")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("issuer", matchesPattern(BASE_URL_REGEX + "/c1"))
                .body("jwks.keys", hasSize(1))
                .body("jwks.keys[0].exp", isA(Integer.class))
                .body("jwks.keys[0]", hasKey("kid"))
                .body("jwks.keys[0].kid", is(not(empty())));
    }

    @DisplayName("sd jwt metadata alternative happy path, variant c1")
    @Test
    @Requirement("PIDI-401")
    @XrayTest(key = "PIDI-1168")
    void test008() {
        new MetadataRequestBuilder()
                .withUrl("/c1/.well-known/jwt-vc-issuer")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("issuer", matchesPattern(BASE_URL_REGEX + "/c1"))
                .body("jwks.keys", hasSize(1))
                .body("jwks.keys[0].exp", isA(Integer.class))
                .body("jwks.keys[0]", hasKey("kid"))
                .body("jwks.keys[0].kid", is(not(empty())));
    }


    @DisplayName("sd jwt metadata alternative path invalid variant, variant c1")
    @Test
    @Requirement("PIDI-401")
    @XrayTest(key = "PIDI-1169")
    void test009() {
        new MetadataRequestBuilder()
                .withUrl("/invalid/.well-known/jwt-vc-issuer")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("'invalid' is not a known flow variant path"));
    }

    @DisplayName("sd jwt metadata invalid variant, variant c1")
    @Test
    @Requirement("PIDI-401")
    @XrayTest(key = "PIDI-1170")
    void test010() {
        new MetadataRequestBuilder()
                .withUrl("/.well-known/jwt-vc-issuer/invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("'invalid' is not a known flow variant path"));
    }
}
