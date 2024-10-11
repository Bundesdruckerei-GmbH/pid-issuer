/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.steps;

import com.nimbusds.jose.jwk.ECKey;
import de.bdr.openid4vc.vci.data.TokenType;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.in.dto.CredentialDto;
import de.bdr.pidi.end2end.requests.AuthorizationRequestBuilder;
import de.bdr.pidi.end2end.requests.CredentialRequestBuilder;
import de.bdr.pidi.end2end.requests.Documentation;
import de.bdr.pidi.end2end.requests.EidRequestBuilder;
import de.bdr.pidi.end2end.requests.FinishAuthorizationRequestBuilder;
import de.bdr.pidi.end2end.requests.PushedAuthorizationRequestBuilder;
import de.bdr.pidi.end2end.requests.RefreshTokenRequestBuilder;
import de.bdr.pidi.end2end.requests.SeedCredentialTokenRequestBuilder;
import de.bdr.pidi.end2end.requests.SessionRequestBuilder;
import de.bdr.pidi.end2end.requests.TokenRequestBuilder;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.Pin;
import de.bdr.pidi.testdata.TestConfig;
import de.bdr.pidi.testdata.TestUtils;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.core.Is.is;

public class Steps {
    private static final String REQUEST_URI = "request_uri";
    private final FlowVariant variant;
    private final List<FlowVariant> refreshTokenVariants = List.of(FlowVariant.C1);
    private final RestDocumentationContextProvider restDocumentation;
    private static final String ISSUER_STATE_REGEX = "[a-zA-Z0-9]{22}";

    public Steps(FlowVariant variant) {
        this(variant, null);
    }

    public Steps(FlowVariant variant, RestDocumentationContextProvider restDocumentation) {
        this.variant = variant;
        this.restDocumentation = restDocumentation;
    }

    public String doPAR(String clientId) {
        return doPAR(clientId, null);
    }

    public String doPAR(String clientId, Documentation documentation) {
        return PushedAuthorizationRequestBuilder.valid(variant, clientId)
                .withDocumentation(prefixWithVariant(documentation))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.CREATED)
                .extract().path(REQUEST_URI);
    }

    public String doPAR() {
        return doPAR((Documentation) null);
    }

    public String doPAR(Documentation documentation) {
        return PushedAuthorizationRequestBuilder.valid(variant, ClientIds.validClientId().toString())
                .withDocumentation(prefixWithVariant(documentation))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.CREATED)
                .extract().path(REQUEST_URI);
    }

    // TODO inconsistent clientID handling in authorize. Most tests pass the clientID to the other requests but not to the authorize

    public String doAuthorize(String requestUri, Documentation documentation) {
        var clientId = ClientIds.validClientId().toString();
        return doAuthorize(clientId, requestUri, documentation);
    }

    public String doAuthorize(String clientId, String requestUri, Documentation documentation) {
        String location = AuthorizationRequestBuilder.valid(variant, clientId, requestUri)
                .getRequestUrl(TestConfig.pidiHostnameFromMock());

        String finishAuthUrl = new EidRequestBuilder(restDocumentation)
                .withTCTokenUrl(location)
                .withPort(TestConfig.getEidMockPort())
                .withHost(TestConfig.getEidMockHostname())
                .withDocumentation(prefixWithVariant(documentation))
                .doRequest().header("Location");
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(finishAuthUrl).build();
        var issuerState = uriComponents.getQueryParams().getFirst("issuer_state");

        assertThat(uriComponents.getPath(), is( "/"+variant.urlPath + "/finish-authorization"));
        assertThat(finishAuthUrl, Matchers.startsWith(TestConfig.pidiBaseUrl() + "/" + variant.urlPath + "/finish-authorization?issuer_state"));

        assertThat(issuerState, Matchers.matchesPattern(ISSUER_STATE_REGEX));
        return issuerState;
    }

    public String doAuthorize(String clientId, String requestUri) {
        return doAuthorize(clientId, requestUri, null);
    }

    /**
     * @param requestUri
     * @return the issuer_state parameter
     */
    public String doAuthorize(String requestUri) {
        return doAuthorize(requestUri, (Documentation) null);
    }

    public Map<String, String> doFinishAuthorization(String issuerState) {
        return doFinishAuthorization(issuerState, null);
    }

    public Map<String, String> doFinishAuthorization(String issuerState, Documentation documentation) {
        var headers = FinishAuthorizationRequestBuilder.valid(variant, issuerState)
                .withDocumentation(prefixWithVariant(documentation))
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.FOUND)
                .extract().headers();
        var code = UriComponentsBuilder.fromHttpUrl(headers.getValue("Location")).build().getQueryParams().get("code").getFirst();
        assertThat(headers.getValue("DPoP-Nonce"), Matchers.matchesPattern(ISSUER_STATE_REGEX));
        return Map.of("code", code, "dpop_nonce", headers.getValue("DPoP-Nonce"));
    }

    public Map<String, String> doTokenRequest(String authorizationCode, String dpopNonce) {
        return doTokenRequest(authorizationCode, dpopNonce, null);
    }

    public Map<String, String> doTokenRequest(ECKey deviceKeyPair, String authorizationCode, String dpopNonce) {
        return doTokenRequest(deviceKeyPair, authorizationCode, dpopNonce, null);
    }

    public Map<String, String> doTokenRequest(String authorizationCode, String dpopNonce, Documentation documentation) {
        return doTokenRequest(TestUtils.DEVICE_KEY_PAIR, authorizationCode, dpopNonce, documentation);
    }

    public Map<String, String> doTokenRequest(ECKey deviceKeyPair, String authorizationCode, String dpopNonce, Documentation documentation) {
        TokenRequestBuilder tokenRequestBuilder = TokenRequestBuilder.valid(variant, deviceKeyPair, dpopNonce)
                .withDocumentation(prefixWithVariant(documentation))
                .withAuthorizationCode(authorizationCode);
        var response = tokenRequestBuilder
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("token_type", is(TokenType.DPOP.getValue()))
                .body("access_token", is(notNullValue()))
                .body("expires_in", isA(Integer.class));

        var jsonPath = response.extract().body().jsonPath();
        var headers = response.extract().headers();
        var values = new HashMap<String, String>();
        values.put("access_token", jsonPath.getString("access_token"));
        values.put("c_nonce", jsonPath.getString("c_nonce"));
        assertThat(headers.getValue("DPoP-Nonce"), Matchers.matchesPattern(ISSUER_STATE_REGEX));
        values.put("dpop_nonce", headers.getValue("DPoP-Nonce"));

        if (refreshTokenVariants.contains(variant)) {
            response.body("refresh_token", is(notNullValue()));
            values.put("refresh_token", jsonPath.getString("refresh_token"));
        }
        return values;
    }

    public HashMap<String, String> doCredentialRequest(CredentialDto.SupportedFormats format, String dpopNonce, String accessToken) {
        return doCredentialRequest(format, dpopNonce, accessToken, null);
    }

    public HashMap<String, String> doCredentialRequest(CredentialDto.SupportedFormats format, String dpopNonce, String accessToken, Documentation documentation) {
        CredentialRequestBuilder credentialRequestBuilder =
                switch (format) {
                    case CredentialDto.SupportedFormats.SD_JWT ->
                            CredentialRequestBuilder.validSdJwt(variant, dpopNonce, accessToken);
                    case CredentialDto.SupportedFormats.MSO_MDOC ->
                            CredentialRequestBuilder.validMdoc(variant, dpopNonce, accessToken);
                    default -> throw new IllegalStateException("Unexpected value: " + format);
                };
        credentialRequestBuilder = credentialRequestBuilder.withDocumentation(prefixWithVariant(documentation));
        var response = credentialRequestBuilder.doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK).header("Content-Type", is("application/json"))
                .body("credential", is(notNullValue()))
                .body("c_nonce", is(notNullValue()))
                .body("c_nonce", matchesPattern(TestUtils.NONCE_REGEX))
                .body("c_nonce_expires_in", isA(Integer.class));
        var body = response.extract().body();
        var values = new HashMap<String, String>();
        values.put("credential", body.jsonPath().getString("credential"));
        values.put("c_nonce", body.jsonPath().getString("c_nonce"));
        values.put("c_nonce_expires_in", body.jsonPath().getString("c_nonce_expires_in"));
        return values;
    }

    public Map<String, String> doSeedCredentialRequest(ECKey deviceKeyPair, String accessToken, String dpopNonce, Pin pin) {
        return doSeedCredentialRequest(deviceKeyPair, accessToken, dpopNonce, pin, null);
    }

    public Map<String, String> doSeedCredentialRequest(ECKey deviceKeyPair, String accessToken, String dpopNonce, Pin pin, Documentation documentation) {
        var requestBuilder = CredentialRequestBuilder.validSeedPid(variant, deviceKeyPair, dpopNonce, accessToken, pin)
                .withDocumentation(prefixWithVariant(documentation));
        var response = requestBuilder
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("credential", is(notNullValue()));
        var jsonPath = response.extract().body().jsonPath();
        return Map.of("credential", jsonPath.getString("credential"));
    }

    public String doSessionRequest() {
        return doSessionRequest(null);
    }

    public String doSessionRequest(Documentation documentation) {
        var requestBuilder = SessionRequestBuilder.valid()
                .withDocumentation(prefixWithVariant(documentation));
        return requestBuilder
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("session_id", is(notNullValue()))
                .extract().body().jsonPath().getString("session_id");
    }

    public Map<String, String> doSeedTokenRequest(ECKey deviceKeyPair, String seedCredential, String clientId, Pin pin) {
        var dpopNonce = doSeedTokenDpopNonceRequest(deviceKeyPair, seedCredential, clientId, pin, null);
        return doSeedTokenRequest(deviceKeyPair, seedCredential, clientId, pin, dpopNonce, null);
    }

    public Map<String, String> doSeedTokenRequest(ECKey deviceKeyPair, String seedCredential, String clientId, Pin pin, String dpopNonce, Documentation documentation) {
        var requestBuilder = SeedCredentialTokenRequestBuilder.valid(variant, deviceKeyPair, clientId, dpopNonce, pin, seedCredential)
                .withDocumentation(prefixWithVariant(documentation));
        var response = requestBuilder
                .doRequest()
                .then().log().ifError()
                .assertThat()
                .status(HttpStatus.OK)
                .body("token_type", is(TokenType.DPOP.getValue()))
                .body("access_token", is(notNullValue()))
                .body("expires_in", isA(Integer.class));

        var jsonPath = response.extract().body().jsonPath();
        var headers = response.extract().headers();
        return Map.of(
                "access_token", jsonPath.getString("access_token"),
                "c_nonce", jsonPath.getString("c_nonce"),
                "dpop_nonce", headers.getValue("DPoP-Nonce"));
    }

    public String doSeedTokenDpopNonceRequest(ECKey deviceKeyPair, String seedCredential, String clientId, Pin pin, Documentation documentation) {
        var requestBuilder = SeedCredentialTokenRequestBuilder.valid(variant, deviceKeyPair, clientId, "dpopNonce", pin, seedCredential)
                .withDocumentation(prefixWithVariant(documentation));
        var response = requestBuilder
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("error", oneOf("use_dpop_nonce", "invalid_dpop_proof"));
        return response.extract().header("DPoP-Nonce");
    }

    public String doRefreshTokenInitRequest(String clientId, String refreshToken, Documentation documentation) {
        var requestBuilder = RefreshTokenRequestBuilder.valid(variant, clientId, null, refreshToken)
                .withDocumentation(prefixWithVariant(documentation));
        var response = requestBuilder.doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.BAD_REQUEST)
                .body("token_type", is(nullValue()))
                .body("access_token", is(nullValue()))
                .header("DPoP-Nonce", is(notNullValue()));
        return response.extract().header("DPoP-Nonce");
    }

    public Map<String, String> doRefreshTokenRequest(String clientId, String refreshToken, String dpopNonce, Documentation documentation) {
        var requestBuilder = RefreshTokenRequestBuilder.valid(variant, clientId, dpopNonce, refreshToken)
                .withDocumentation(prefixWithVariant(documentation));
        var response = requestBuilder.doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("token_type", is(TokenType.DPOP.getValue()))
                .body("access_token", is(notNullValue()))
                .body("expires_in", isA(Integer.class))
                .header("DPoP-Nonce", is(notNullValue()))
                .header("DPoP-Nonce", not(dpopNonce));
        var jsonPath = response.extract().body().jsonPath();
        var headers = response.extract().headers();

        return Map.of(
                "access_token", jsonPath.getString("access_token"),
                "c_nonce", jsonPath.getString("c_nonce"),
                "dpop_nonce", headers.getValue("DPoP-Nonce"));
    }

    public Documentation prefixWithVariant(Documentation documentation) {
        if (documentation == null) {
            return null;
        }
        return new Documentation(variant.urlPath + "/" + documentation.name());
    }
}
