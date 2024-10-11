/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.CredentialRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.Pin;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static de.bdr.pidi.end2end.requests.RequestBuilder.objectMapper;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

class B1SeedCredentialRequestTest extends RemoteTest {

    private final Steps steps = new Steps(FlowVariant.B1);

    @DisplayName("Seed credential endpoint happy path, variant b1")
    @Test
    @Requirement({"PIDI-828", "PIDI-1688", "PIDI-1507", "PIDI-1511"})
    @XrayTest(key = "PIDI-1658")
    void test001() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);

        CredentialRequestBuilder
                .validSeedPid(FlowVariant.B1, deviceKeyPair, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"), pin)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credential", is(notNullValue()))
        ;
    }

    @DisplayName("Seed credential endpoint unsupported format, variant b1")
    @Test
    @Requirement({"PIDI-828", "PIDI-1511"})
    @XrayTest(key = "PIDI-1656")
    void test002() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var body = objectMapper.createObjectNode()
                .put("format", "unsupported_format")
                .put("vct", "https://example.bmi.bund.de/credential/pid/1.0");
        CredentialRequestBuilder
                .validSeedPid(FlowVariant.B1, deviceKeyPair, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"), pin)
                .withJsonBody(body).doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("unsupported_credential_format"))
                .body("error_description", is("Credential format \"unsupported_format\" not supported"));
    }

    @DisplayName("Seed credential endpoint missing format, variant b1")
    @Test
    @Requirement({"PIDI-828", "PIDI-1511"})
    @XrayTest(key = "PIDI-1657")
    void test003() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        CredentialRequestBuilder
                .validSeedPid(FlowVariant.B1, deviceKeyPair, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"), pin)
                .withRemovedJsonBodyProperty("format")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("format parameter is missing"));
    }
    @DisplayName("Seed credential endpoint empty format, variant b1")
    @Test
    @Requirement({"PIDI-828", "PIDI-1511"})
    @XrayTest(key = "PIDI-2175")
    void test005() {
        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var body = objectMapper.createObjectNode()
                .put("format", "")
                .put("vct", "https://example.bmi.bund.de/credential/pid/1.0");
        CredentialRequestBuilder
                .validSeedPid(FlowVariant.B1, deviceKeyPair, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"), pin)
                .withJsonBody(body).doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", is("format parameter is missing"));
    }

    @DisplayName("Seed credential endpoint missing pin derived eph key pop, variant b1")
    @Test
    @Requirement({"PIDI-1507"})
    @XrayTest(key = "PIDI-2136")
    void test004() {

        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);

        CredentialRequestBuilder
                .validSeedPid(FlowVariant.B1, deviceKeyPair, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"), pin)
                .withRemovedJsonBodyProperty("pin_derived_eph_key_pop")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_credential_request"))
                .body("error_description", containsString("Field 'pin_derived_eph_key_pop' is required"))
        ;
    }
    @DisplayName("Seed credential endpoint empty pin derived eph key pop, variant b1")
    @Test
    @Requirement({"PIDI-1507"})
    @XrayTest(key = "PIDI-2174")
    void test006() {

        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var body = objectMapper.createObjectNode()
                .put("format", "seed_credential")
                .put("pin_derived_eph_key_pop", "");
        CredentialRequestBuilder
                .validSeedPid(FlowVariant.B1, deviceKeyPair, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"), pin)
                .withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT could not be parsed"))
        ;
    }
    @DisplayName("Seed credential endpoint unsupported pin derived eph key pop, variant b1")
    @Test
    @Requirement({"PIDI-1507"})
    @XrayTest(key = "PIDI-2173")
    void test007() {

        var clientId = ClientIds.validClientId().toString();
        var deviceKeyPair = TestUtils.generateEcKey();
        var requestUri = steps.doPAR(clientId);
        var issuerState = steps.doAuthorize(requestUri);
        var faResponse = steps.doFinishAuthorization(issuerState);
        var tokenResponse = steps.doTokenRequest(deviceKeyPair, faResponse.get("code"), faResponse.get("dpop_nonce"));
        var pin = Pin.createPin(tokenResponse.get("c_nonce"), deviceKeyPair);
        var body = objectMapper.createObjectNode()
                .put("format", "seed_credential")
                .put("pin_derived_eph_key_pop", "unsupported");
        CredentialRequestBuilder
                .validSeedPid(FlowVariant.B1, deviceKeyPair, tokenResponse.get("dpop_nonce"), tokenResponse.get("access_token"), pin)
                .withJsonBody(body)
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", is("invalid_proof"))
                .body("error_description", is("Proof JWT could not be parsed"))
        ;
    }
}
