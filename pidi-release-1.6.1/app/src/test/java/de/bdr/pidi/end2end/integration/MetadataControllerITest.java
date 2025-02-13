/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.integration;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static de.bdr.pidi.base.PidDataConst.MDOC_TYPE;
import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.core.Is.is;

@Tag("e2e")
class MetadataControllerITest extends RestAssuredWebTest {
    private static final String BASE_URL_REGEX = "https?://\\w++(?:\\.[\\w\\-]+)*+(?::\\d+)?";

    @Test
    @DisplayName("Verify the positive call to authorization metadata endpoint, variant c1")
    void test001() {
        String[] dpopSigningAlgs = JWSAlgorithm.Family.SIGNATURE.stream().map(Algorithm::toString).toArray(String[]::new);

        given()
                .when()
                .get("/c1/.well-known/oauth-authorization-server")
                .then()
                .assertThat()
                .statusCode(is(200))
                .body("authorization_endpoint", matchesPattern(BASE_URL_REGEX + "/c1/authorize"))
                .body("dpop_signing_alg_values_supported", contains(dpopSigningAlgs))
        ;
    }

    @Test
    @DisplayName("Verify the positive call to authorization metadata endpoint, variant c1, alternative path")
    void test002() {
        given()
                .when()
                .get("/.well-known/oauth-authorization-server/c1")
                .then()
                .assertThat()
                .statusCode(is(200))
                .body("authorization_endpoint", matchesPattern(BASE_URL_REGEX + "/c1/authorize"))
        ;
    }

    @Test
    @DisplayName("Verify the positive call to credential metadata endpoint, variant c")
    void test003() {
        given()
                .when()
                .get("/c/.well-known/openid-credential-issuer")
                .then()
                .assertThat()
                .statusCode(is(200))
                .body("credential_endpoint", matchesPattern(BASE_URL_REGEX + "/c/credential"))
                .body("batch_credential_issuance.batch_size", is(42))
                .body("credential_configurations_supported.size()", is(2))
                .body("credential_configurations_supported.pid-sd-jwt.format", is("vc+sd-jwt"))
                .body("credential_configurations_supported.pid-sd-jwt.vct", matchesPattern(BASE_URL_REGEX + "/credentials/pid/1.0"))
                .body("credential_configurations_supported.pid-mso-mdoc.format", is("mso_mdoc"))
                .body("credential_configurations_supported.pid-mso-mdoc.doctype", is(MDOC_TYPE))
        ;
    }

    @Test
    @DisplayName("Verify the invalid call to authorization metadata endpoint, unknown variant")
    void test004_fail() {
        var unknownPath = "unknown";
        given()
                .when()
                .get("/" + unknownPath + "/.well-known/oauth-authorization-server")
                .then()
                .assertThat()
                .statusCode(is(400))
                .body("error", is("invalid_request"))
                .body("error_description", is("'" + unknownPath + "' is not a known flow variant path"))
        ;
    }

    @Test
    @DisplayName("Verify the invalid call to credential metadata endpoint, not allowed variant d")
    void test005_fail() {
        var notAllowedPath = "d";
        given()
                .when()
                .get("/" + notAllowedPath + "/.well-known/openid-credential-issuer")
                .then()
                .assertThat()
                .statusCode(is(400))
                .body("error", is("invalid_request"))
                .body("error_description", is("'" + notAllowedPath + "' is not an allowed flow variant path"))
        ;
    }

    @Test
    @DisplayName("Verify the positive call to jwt metadata endpoint, variant c")
    void test006() {
        given()
                .when()
                .get("/c/.well-known/jwt-vc-issuer")
                .then()
                .assertThat()
                .statusCode(is(200))
                .body("issuer", matchesPattern(BASE_URL_REGEX + "/c"))
                .body("jwks.keys", hasSize(1))
                .body("jwks.keys[0]", hasKey("kid"))
        ;
    }

    @Test
    @DisplayName("Verify the invalid call to jwt metadata endpoint, unknown variant")
    void test007_fail() {
        var unknownPath = "unknown";
        given()
                .when()
                .get("/.well-known/jwt-vc-issuer/" + unknownPath)
                .then()
                .assertThat()
                .statusCode(is(400))
                .statusCode(is(400))
                .body("error", is("invalid_request"))
                .body("error_description", is("'" + unknownPath + "' is not a known flow variant path"))
        ;
    }

    @Test
    @DisplayName("Verify the positive call to vct metadata endpoint")
    void test008() {
        given()
                .when()
                .get("/credentials/pid/1.0")
                .then()
                .assertThat()
                .statusCode(is(200))
                .body("vct", matchesPattern(BASE_URL_REGEX + "/credentials/pid/1.0"))
        ;
    }
}
