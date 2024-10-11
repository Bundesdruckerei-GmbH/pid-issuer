/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.CredentialRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.TestUtils;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static de.bdr.pidi.testdata.TestUtils.OBJECT_MAPPER;
import static de.bdr.pidi.testdata.TestUtils.getDisclosures;
import static de.bdr.pidi.testdata.TestUtils.toJsonNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class C2CredentialTest extends RemoteTest {

    public static final FlowVariant FLOW_VARIANT = FlowVariant.C2;
    private final Steps steps = new Steps(FLOW_VARIANT);


    @DisplayName("Credential endpoint happy path, variant c2")
    @Requirement("PIDI-1472")
    @XrayTest(key = "PIDI-1521")
    @Test
    void test001() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));

        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK).header("Content-Type", is("application/json"))
                .body("credential", is(notNullValue()))
                .body("c_nonce", is(notNullValue()))
                .body("c_nonce", matchesPattern(TestUtils.NONCE_REGEX))
                .body("c_nonce_expires_in", isA(Integer.class));
    }

    @DisplayName("Credential endpoint credential format unsupported, variant c2")
    @Requirement("PIDI-1472")
    @XrayTest(key = "PIDI-1545")
    @Test
    void test002() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder.validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withFormatAndVct("invalid", null)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_credential_format"))
                .body("error_description", is("Credential format \"invalid\" not supported"));
    }

    @DisplayName("Credential endpoint credential type unsupported, variant c2")
    @Requirement("PIDI-1472")
    @XrayTest(key = "PIDI-1537")
    @Test
    void test003() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));

        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withFormatAndVct("vc+sd-jwt", "invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_credential_type"))
                .body("error_description", is("Credential type \"invalid\" not supported"));
    }

    @DisplayName("Credential endpoint empty access token, variant c2")
    @Requirement("PIDI-1472")
    @XrayTest(key = "PIDI-1562")
    @Test
    void test004() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));

        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), "")
                .doRequest()
                .then()
                .assertThat().status(HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("Credential endpoint invalid access token, variant c2")
    @Requirement("PIDI-1472")
    @XrayTest(key = "PIDI-1552")
    @Test
    void test005() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));

        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), "foo")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", containsString("error=\"invalid_token\""));
    }

    @DisplayName("Credential endpoint invalid dpop, variant c2")
    @Requirement("PIDI-1472")
    @XrayTest(key = "PIDI-1581")
    @Test
    void test006() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));

        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, "foo", tokenResponse.get("access_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", containsString("error=\"use_dpop_nonce\""))
                .header("WWW-Authenticate", containsString("error_description=\"DPoP nonce is invalid\""))
                .header("WWW-Authenticate", not(containsString("DPoP-Nonce:")))
                .header("DPoP-Nonce", is(notNullValue()));

    }

    @DisplayName("Credential endpoint request without dpop, variant c2")
    @Requirement("PIDI-1472")
    @XrayTest(key = "PIDI-1572")
    @Test
    void test007() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));

        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("c_nonce"), tokenResponse.get("access_token"))
                .withDpopHeader(FlowVariant.C2, tokenResponse.get("access_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", containsString("error=\"use_dpop_nonce\""))
                .header("WWW-Authenticate", containsString("error_description=\"nonce value missing\""))
                .header("WWW-Authenticate", not(containsString("DPoP-Nonce:")))
                .header("DPoP-Nonce", notNullValue());

    }

    @Test
    @DisplayName("Credential request happy path with mdoc, variant c2")
    @Requirement("PIDI-1472")
    @XrayTest(key = "PIDI-1544")
    void test008() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validMdoc(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credential", is(notNullValue()))
        ;
    }

    @Test
    @DisplayName("Credential request, unsupported doctype, variant c2")
    @Requirement("PIDI-1472")
    @XrayTest(key = "PIDI-1536")
    void test009() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        var body = CredentialRequestBuilder.validMdocRequestBody();
        body.put("doctype", "unsupported");
        CredentialRequestBuilder
                .validMdoc(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_credential_type"))
                .body("error_description", is("Credential type \"unsupported\" not supported"))
        ;
    }

    @Test
    @DisplayName("Credential request, missing doctype, variant c2")
    @Requirement("PIDI-1472")
    @XrayTest(key = "PIDI-1649")
//    @ExpectedToFail("PIDI-1059")
    void test010() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validMdoc(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withRemovedJsonBodyProperty("doctype")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("doctype parameter is missing"))
        ;
    }

    @Test
    @DisplayName("Credential request, missing format, variant c2")
    @Requirement("PIDI-1472")
    @XrayTest(key = "PIDI-1555")
//    @ExpectedToFail("PIDI-1059")
    void test011() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        CredentialRequestBuilder
                .validMdoc(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withRemovedJsonBodyProperty("format")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("format parameter is missing"))
        ;
    }

    @Test
    @DisplayName("Credential request, unsupported format, variant c2")
    @Requirement("PIDI-1472")
    @XrayTest(key = "PIDI-1580")
    void test012() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        var body = CredentialRequestBuilder.validMdocRequestBody();
        body.put("format", "unsupported");
        CredentialRequestBuilder
                .validMdoc(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_credential_format"))
                .body("error_description", is("Credential format \"unsupported\" not supported"))
        ;
    }

    @DisplayName("Credential endpoint all claims check, variant c2")
    @Requirement("PIDI-251")
    @XrayTest(key = "PIDI-1525")
    @Test
    void test013() {
        java.util.Base64.Decoder urlDecoder = java.util.Base64.getUrlDecoder();
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        var now = Instant.now();

        var body = CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .doRequest().body();
        var jsonPath = body.jsonPath();
        String[] splitString = jsonPath.getString("credential").split("\\.");
        String base64EncodedBody = splitString[1];
        String base64EncodedSignature = splitString[2];
        List<String> disclosures = getDisclosures(base64EncodedSignature);
        HashMap<String, JsonNode> parsedDisclosuresMap = disclosures.stream()
                .map(disclosureJson -> new String(urlDecoder.decode(disclosureJson)))
                .map(toJsonNode())
                .collect(Collectors.toMap(disclosureJsonNode -> disclosureJsonNode.get(1).asText(),
                        disclosureJsonNode -> disclosureJsonNode.get(2),
                        (a, b) -> new ArrayNode(OBJECT_MAPPER.getNodeFactory()).add(a).add(b),
                        HashMap::new));
        String jsonString = new String(Base64.decodeBase64(base64EncodedBody), StandardCharsets.UTF_8);
        StringReader reader = new StringReader(jsonString);
        JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
        assertAll(
                () -> assertEquals("MUSTERMANN", parsedDisclosuresMap.get("family_name").asText()),
                () -> assertEquals("ERIKA", parsedDisclosuresMap.get("given_name").asText()),
                () -> assertEquals("1964-08-12", parsedDisclosuresMap.get("birthdate").asText()),
                () -> assertEquals("1964", parsedDisclosuresMap.get("age_birth_year").asText()),
                () -> assertTrue(parsedDisclosuresMap.get("12").asBoolean()),
                () -> assertTrue(parsedDisclosuresMap.get("14").asBoolean()),
                () -> assertTrue(parsedDisclosuresMap.get("16").asBoolean()),
                () -> assertTrue(parsedDisclosuresMap.get("18").asBoolean()),
                () -> assertTrue(parsedDisclosuresMap.get("21").asBoolean()),
                () -> assertFalse(parsedDisclosuresMap.get("65").asBoolean()),
                () -> assertEquals(2, parsedDisclosuresMap.get("locality").size()),
                () -> assertEquals("BERLIN", parsedDisclosuresMap.get("locality").get(0).asText()),
                () -> assertEquals("KÖLN", parsedDisclosuresMap.get("locality").get(1).asText()),
                () -> assertEquals("DE", parsedDisclosuresMap.get("country").asText()),
                () -> assertEquals("51147", parsedDisclosuresMap.get("postal_code").asText()),
                () -> assertEquals("HEIDESTRAẞE 17", parsedDisclosuresMap.get("street_address").asText()),
                () -> assertNotNull(jsonObject.get("issuing_authority")),
                () -> assertEquals("DE", jsonObject.get("issuing_authority").isJsonNull() ? "" : jsonObject.get("issuing_authority").getAsString()),
                () -> assertNotNull(jsonObject.get("issuing_country")),
                () -> assertEquals("DE", jsonObject.get("issuing_country").getAsString()),
                () -> assertNotNull(jsonObject.get("iat")),
                () -> assertThat(Instant.ofEpochSecond(jsonObject.get("iat").getAsLong())).isCloseTo(now, within(5, ChronoUnit.SECONDS)),
                () -> assertNotNull(jsonObject.get("exp")),
                () -> assertThat(Instant.ofEpochSecond(jsonObject.get("exp").getAsLong())).isCloseTo(now, within(14, ChronoUnit.DAYS)),
                () -> assertThat(jsonObject.get("iss").getAsString()).isNotNull().matches("http.*/" + FLOW_VARIANT.urlPath));
    }

    @DisplayName("Credential endpoint with unexpected proof, variant c2")
    @Requirement("PIDI-1472")
    @XrayTest(key = "PIDI-1548")
    @Test
    void test014() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));

        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProof(FLOW_VARIANT, clientId, TestUtils.ISSUER_IDENTIFIER_AUDIENCE, Instant.now(), tokenResponse.get("c_nonce"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("Neither proof nor proofs expected"));
    }

    @DisplayName("Credential endpoint with unexpected proofs, variant c2")
    @Requirement("PIDI-1472")
    @XrayTest(key = "PIDI-1540")
    @Test
    void test015() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));

        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .withProofs(FlowVariant.C1, clientId, Instant.now(), tokenResponse.get("c_nonce"), 2)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("Neither proof nor proofs expected"));
    }

    @DisplayName("Credential endpoint dpop nonce expired, variant c2")
    @Requirement("PIDI-266")
    @SuppressWarnings("java:S2925")
    @XrayTest(key = "PIDI-1565")
    @Test
    void test016() {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        CredentialRequestBuilder
                .validSdJwt(FLOW_VARIANT, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", containsString("error=\"invalid_dpop_proof\""))
                .header("WWW-Authenticate", containsString("error_description=\"DPoP nonce is expired\""));
    }
}
