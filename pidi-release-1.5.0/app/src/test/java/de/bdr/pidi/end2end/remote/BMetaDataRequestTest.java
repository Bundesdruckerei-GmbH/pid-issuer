/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
import static de.bdr.pidi.base.PidDataConst.SD_JWT_VCTYPE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

class BMetaDataRequestTest extends RemoteTest {
    private static final String BASE_URL_REGEX = "https?://\\w++(?:\\.[\\w\\-]+)*+(?::\\d+)?";

    @DisplayName("Credential metadata endpoint happy path, variant b")
    @Test
    @Requirement({"PIDI-1142","PIDI-1512"})
    @XrayTest(key = "PIDI-1356")
    void test001() {
        new MetadataRequestBuilder()
                .withUrl("/b/.well-known/openid-credential-issuer")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credential_endpoint", matchesPattern(BASE_URL_REGEX + "/b/credential"))
                .body("credential_issuer", matchesPattern(BASE_URL_REGEX + "/b"))
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
                .body("credential_configurations_supported.pid-sd-jwt.credential_signing_alg_values_supported[0]", is("DVS-P256-SHA256-HS256"))
                .body("credential_configurations_supported.pid-sd-jwt.format", is("vc+sd-jwt"))
                .body("credential_configurations_supported.pid-sd-jwt.vct", is(SD_JWT_VCTYPE))
                .body("credential_configurations_supported.pid-sd-jwt.proof_types_supported.jwt.proof_signing_alg_values_supported.size()", is(1))
                .body("credential_configurations_supported.pid-sd-jwt.proof_types_supported.jwt.proof_signing_alg_values_supported[0]", is("ES256"))
                .body("credential_configurations_supported.pid-sd-jwt.proof_types_supported", is(notNullValue()))
                .body("credential_configurations_supported.pid-mso-mdoc.format", is("mso_mdoc_authenticated_channel"))
                .body("credential_configurations_supported.pid-mso-mdoc.doctype", is(MDOC_TYPE))
                .body("credential_configurations_supported.pid-mso-mdoc.scope", is("pid"))
                .body("credential_configurations_supported.pid-mso-mdoc.cryptographic_binding_methods_supported.size()", is(1))
                .body("credential_configurations_supported.pid-mso-mdoc.cryptographic_binding_methods_supported[0]", is("cose_key"))
                .body("credential_configurations_supported.pid-mso-mdoc.credential_signing_alg_values_supported.size()", is(1))
                .body("credential_configurations_supported.pid-mso-mdoc.credential_signing_alg_values_supported[0]", is("HS256"))
                .body("credential_configurations_supported.pid-mso-mdoc.proof_types_supported", is(nullValue()));
    }

    @Test
    @DisplayName("Authorization Server Metadata happy path, variant b")
    @Requirement("PIDI-243")
    @XrayTest(key = "PIDI-1366")
    void test002() {
        String[] dpopSigningAlgs = JWSAlgorithm.Family.SIGNATURE.stream().map(Algorithm::toString).toArray(String[]::new);
        new MetadataRequestBuilder()
                .withUrl("/b/.well-known/oauth-authorization-server")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("issuer", matchesPattern(BASE_URL_REGEX + "/b"))
                .body("authorization_endpoint", matchesPattern(BASE_URL_REGEX + "/b/authorize"))
                .body("token_endpoint", matchesPattern(BASE_URL_REGEX + "/b/token"))
                .body("pushed_authorization_request_endpoint", matchesPattern(BASE_URL_REGEX + "/b/par"))
                .body("require_pushed_authorization_requests", is(true))
                .body("token_endpoint_auth_methods_supported.size()", is(1))
                .body("token_endpoint_auth_methods_supported[0]", is("none"))
                .body("response_types_supported.size()", is(1))
                .body("response_types_supported[0]", is("code"))
                .body("code_challenge_methods_supported.size()", is(1))
                .body("code_challenge_methods_supported[0]", is("S256"))
                .body("dpop_signing_alg_values_supported", contains(dpopSigningAlgs));
    }

    @DisplayName("Authorization Server Metadata alternative happy path, variant b")
    @Test
    @Requirement("PIDI-243")
    @XrayTest(key = "PIDI-1367")
    void test003() {
        String[] dpopSigningAlgs = JWSAlgorithm.Family.SIGNATURE.stream().map(Algorithm::toString).toArray(String[]::new);
        new MetadataRequestBuilder()
                .withUrl("/.well-known/oauth-authorization-server/b")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("issuer", matchesPattern(BASE_URL_REGEX + "/b"))
                .body("authorization_endpoint", matchesPattern(BASE_URL_REGEX + "/b/authorize"))
                .body("token_endpoint", matchesPattern(BASE_URL_REGEX + "/b/token"))
                .body("pushed_authorization_request_endpoint", matchesPattern(BASE_URL_REGEX + "/b/par"))
                .body("require_pushed_authorization_requests", is(true))
                .body("token_endpoint_auth_methods_supported.size()", is(1))
                .body("token_endpoint_auth_methods_supported[0]", is("none"))
                .body("response_types_supported.size()", is(1))
                .body("response_types_supported[0]", is("code"))
                .body("code_challenge_methods_supported.size()", is(1))
                .body("code_challenge_methods_supported[0]", is("S256"))
                .body("dpop_signing_alg_values_supported", contains(dpopSigningAlgs));
    }

    @DisplayName("Credential metadata endpoint unknow flow variant, variant b")
    @Test
    @Requirement("PIDI-1142")
    @XrayTest(key = "PIDI-1376")
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

    @DisplayName("Authorization Server Metadata invalid flow variant, variant b")
    @Test
    @Requirement("PIDI-243")
    @XrayTest(key = "PIDI-1368")
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

    @DisplayName("Authorization Server Metadata alternative invalid flow variant, variant b")
    @Test
    @Requirement("PIDI-243")
    @XrayTest(key = "PIDI-1369")
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
}
