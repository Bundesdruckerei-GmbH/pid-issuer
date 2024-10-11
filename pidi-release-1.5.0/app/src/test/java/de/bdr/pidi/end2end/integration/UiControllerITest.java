/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.integration;

import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;

@Tag("e2e")
class UiControllerITest extends RestAssuredWebTest {
    @Autowired
    private AuthorizationConfiguration pidiConfiguration;

    @Test
    @DisplayName("Verify the index page exist")
    void test001() {
        given()
                .when()
                .get("/")
                .then()
                .assertThat()
                .statusCode(is(200))
        ;
    }

    @Test
    @DisplayName("Verify the variant-c page exist and returned the correct credential_issuer url")
    void test002() {
        given()
                .when()
                .get("/variant-c")
                .then()
                .assertThat()
                .statusCode(is(200))
                .body(containsString(URLEncoder.encode("\"credential_issuer\":\"" + pidiConfiguration.getBaseUrl() + "/c\"", StandardCharsets.UTF_8)))
                .body(containsString(URLEncoder.encode("\"credential_configuration_ids\":[\"pid-sd-jwt\"]", StandardCharsets.UTF_8)))
                .body(containsString(URLEncoder.encode("\"credential_configuration_ids\":[\"pid-mso-mdoc\"]", StandardCharsets.UTF_8)))
        ;
    }

    @Test
    @DisplayName("Verify the variant-c1 page exist and returned the correct credential_issuer url")
    void test003() {
        given()
                .when()
                .get("/variant-c1")
                .then()
                .assertThat()
                .statusCode(is(200))
                .body(containsString(URLEncoder.encode("\"credential_issuer\":\"" + pidiConfiguration.getBaseUrl() + "/c1\"", StandardCharsets.UTF_8)))
                .body(containsString(URLEncoder.encode("\"credential_configuration_ids\":[\"pid-sd-jwt\"]", StandardCharsets.UTF_8)))
                .body(containsString(URLEncoder.encode("\"credential_configuration_ids\":[\"pid-mso-mdoc\"]", StandardCharsets.UTF_8)))
        ;
    }

    @Test
    @DisplayName("Verify the variant-c2 page exist and returned the correct credential_issuer url")
    void test011() {
        given()
                .when()
                .get("/variant-c2")
                .then()
                .assertThat()
                .statusCode(is(200))
                .body(containsString(URLEncoder.encode("\"credential_issuer\":\"" + pidiConfiguration.getBaseUrl() + "/c2\"", StandardCharsets.UTF_8)))
                .body(containsString(URLEncoder.encode("\"credential_configuration_ids\":[\"pid-sd-jwt\"]", StandardCharsets.UTF_8)))
                .body(containsString(URLEncoder.encode("\"credential_configuration_ids\":[\"pid-mso-mdoc\"]", StandardCharsets.UTF_8)))
        ;
    }

    @Test
    @DisplayName("Verify the privacy page exist")
    void test004() {
        given()
                .when()
                .get("/privacy-terms")
                .then()
                .assertThat()
                .statusCode(is(200))
        ;
    }

    @Test
    @DisplayName("Verify the releases page exist")
    void test005() {
        given()
                .when()
                .get("/releases")
                .then()
                .assertThat()
                .statusCode(is(200))
        ;
    }

    @Test
    @DisplayName("Verify the licence page exist")
    void test006() {
        given()
                .when()
                .get("/licence")
                .then()
                .assertThat()
                .statusCode(is(200))
        ;
    }

    @Test
    @DisplayName("Verify the SD-JWT page exist")
    void test007() {
        given()
                .when()
                .get("/sdjwt")
                .then()
                .assertThat()
                .statusCode(is(200))
        ;
    }

    @Test
    @DisplayName("Verify the MSO-MDOC page exist")
    void test008() {
        given()
                .when()
                .get("/msomdoc")
                .then()
                .assertThat()
                .statusCode(is(200))
        ;
    }

    @Test
    @DisplayName("Verify that the variant-b page exists and contains the flow's base URL")
    void test009() {
        given()
                .when()
                .get("/variant-b")
                .then()
                .assertThat()
                .statusCode(is(200))
                .body(containsString("https://demo.pid-issuer.bundesdruckerei.de/b"))
        ;
    }
    @Test
    @DisplayName("Verify that the variant-b1 page exists and contains the flow's base URL")
    void test010() {
        given()
                .when()
                .get("/variant-b1")
                .then()
                .assertThat()
                .statusCode(is(200))
                .body(containsString("https://demo.pid-issuer.bundesdruckerei.de/b1"))
        ;
    }
}
