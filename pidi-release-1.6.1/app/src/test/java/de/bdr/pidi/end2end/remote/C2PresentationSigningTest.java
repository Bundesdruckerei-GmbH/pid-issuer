/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.in.dto.CredentialDto;
import de.bdr.pidi.end2end.requests.PresentationSigningRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import de.bdr.pidi.testdata.ClientIds;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Map;

import static de.bdr.pidi.end2end.requests.PresentationSigningRequestBuilder.getJwsObjectSigningInput;
import static de.bdr.pidi.end2end.requests.RequestBuilder.objectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

class C2PresentationSigningTest extends RemoteTest {
    private final Steps steps = new Steps(FlowVariant.C2);

    @Test
    @DisplayName("presentation-signing request mdoc happy path, variant c2")
    @Requirement("PIDI-1435")
    @XrayTest(key = "PIDI-1906")
    void test001() {
        var response = prepareTest(CredentialDto.SupportedFormats.MSO_MDOC);

        PresentationSigningRequestBuilder
                .valid("payload", response.get("access_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("signature_bytes", is(notNullValue()))
        ;
    }

    @Test
    @DisplayName("presentation-signing request mdoc empty hash, variant c2")
    @Requirement("PIDI-1435")
    @XrayTest(key = "PIDI-1910")
    void test002() {
        var response = prepareTest(CredentialDto.SupportedFormats.MSO_MDOC);

        var body = objectMapper.createObjectNode().put("hash_bytes", "");
        PresentationSigningRequestBuilder
                .valid("foo", response.get("access_token")).withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Hash bytes missing"));
    }

    @Test
    @DisplayName("presentation-signing request mdoc missing hash, variant c2")
    @Requirement("PIDI-1435")
    @XrayTest(key = "PIDI-1911")
    void test003() {
        var response = prepareTest(CredentialDto.SupportedFormats.MSO_MDOC);

        PresentationSigningRequestBuilder
                .valid("foo", response.get("access_token")).withRemovedJsonBodyProperty("hash_bytes")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Hash bytes missing"));
    }

    @Test
    @DisplayName("presentation-signing request mdoc invalid hash, variant c2")
    @Requirement("PIDI-1435")
    @XrayTest(key = "PIDI-1909")
    void test004() {
        var response = prepareTest(CredentialDto.SupportedFormats.MSO_MDOC);

        var body = objectMapper.createObjectNode().put("hash_bytes", "invalid");
        PresentationSigningRequestBuilder
                .valid("foo", response.get("access_token")).withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Hash bytes invalid"));
    }

    @Test
    @DisplayName("presentation-signing request mdoc invalid token, variant c2")
    @Requirement("PIDI-1435")
    @XrayTest(key = "PIDI-1916")
    void test005() {
        prepareTest(CredentialDto.SupportedFormats.MSO_MDOC);

        PresentationSigningRequestBuilder
                .valid("payload", "invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("presentation-signing request sdjwt happy path, variant c2")
    @Requirement("PIDI-1435")
    @XrayTest(key = "PIDI-1965")
    void test006() throws JOSEException, ParseException {
        var response = prepareTest(CredentialDto.SupportedFormats.SD_JWT);

        String[] splitString = response.get("credential").split("\\.");
        String base64EncodedBody = splitString[1];
        String jsonString = new String(Base64.decodeBase64(base64EncodedBody), StandardCharsets.UTF_8);
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        var presentationSigningResponse = PresentationSigningRequestBuilder
                .valid("payload", response.get("access_token"))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("signature_bytes", is(notNullValue()));
        String signatureString = presentationSigningResponse.extract().body().jsonPath().get("signature_bytes");
        var serializedComposedJWS = new String(getJwsObjectSigningInput("payload"), StandardCharsets.UTF_8) + "." + signatureString;
        var verifier = new ECDSAVerifier(ECKey.parse(jsonObject.get("cnf").getAsJsonObject().get("jwk").toString()));
        assertThat(JWSObject.parse(serializedComposedJWS).verify(verifier)).isTrue();
    }

    @Test
    @DisplayName("presentation-signing request sdjwt empty hash, variant c2")
    @Requirement("PIDI-1435")
    @XrayTest(key = "PIDI-1962")
    void test008() {
        var response = prepareTest(CredentialDto.SupportedFormats.SD_JWT);

        var body = objectMapper.createObjectNode().put("hash_bytes", "");
        PresentationSigningRequestBuilder
                .valid("foo", response.get("access_token")).withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Hash bytes missing"));
    }

    @Test
    @DisplayName("presentation-signing request sdjwt missing hash, variant c2")
    @Requirement("PIDI-1435")
    @XrayTest(key = "PIDI-1963")
    void test009() {
        var response = prepareTest(CredentialDto.SupportedFormats.SD_JWT);

        PresentationSigningRequestBuilder
                .valid("foo", response.get("access_token")).withRemovedJsonBodyProperty("hash_bytes")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Hash bytes missing"));
    }

    @Test
    @DisplayName("presentation-signing request sdwt invalid hash, variant c2")
    @Requirement("PIDI-1435")
    @XrayTest(key = "PIDI-1961")
    void test010() {
        var response = prepareTest(CredentialDto.SupportedFormats.SD_JWT);

        var body = objectMapper.createObjectNode().put("hash_bytes", "invalid");
        PresentationSigningRequestBuilder
                .valid("foo", response.get("access_token")).withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_request"))
                .body("error_description", is("Hash bytes invalid"));
    }

    @Test
    @DisplayName("presentation-signing request sdjwt invalid token, variant c2")
    @Requirement("PIDI-1435")
    @XrayTest(key = "PIDI-1960")
    void test011() {
        prepareTest(CredentialDto.SupportedFormats.SD_JWT);

        PresentationSigningRequestBuilder
                .valid("payload", "invalid")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.UNAUTHORIZED);
    }

    private Map<String, String> prepareTest(CredentialDto.SupportedFormats format) {
        var clientId = ClientIds.validClientId().toString();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(faResponse.get("code"), faResponse.get("dpop_nonce"));
        var response = steps.doCredentialRequest(format, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"));

        response.put("access_token", tokenResponse.get("access_token"));
        return response;
    }
}
