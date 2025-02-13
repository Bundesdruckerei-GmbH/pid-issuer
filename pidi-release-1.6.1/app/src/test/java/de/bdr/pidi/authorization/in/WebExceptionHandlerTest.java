/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;
import de.bdr.openid4vc.vci.data.TokenType;
import de.bdr.pidi.authorization.core.exception.FinishAuthException;
import de.bdr.pidi.authorization.core.exception.ParameterTooLongException;
import de.bdr.pidi.authorization.core.exception.UnauthorizedException;
import de.bdr.pidi.authorization.core.particle.ClientNotRegisteredException;
import de.bdr.pidi.authorization.core.particle.IdentificationFailedException;
import de.bdr.pidi.authorization.core.particle.UseDpopNonceException;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import de.bdr.pidi.base.PidServerException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebExceptionHandlerTest {
    static final String DPOP_SIGNING_ALGS = JWSAlgorithm.Family.SIGNATURE.stream().map(Algorithm::toJSONString).toList().toString();
    private final WebExceptionHandler webExceptionHandlerUT = new WebExceptionHandler(new ObjectMapper());

    @DisplayName("Verify mapping of ClientNotRegisteredException")
    @Test
    void verifyMappingOfClientNotRegisteredException() {
        UUID clientId = UUID.randomUUID();
        ClientNotRegisteredException clientNotRegisteredException = new ClientNotRegisteredException("Client Id not registered: " + clientId);
        var result = webExceptionHandlerUT.handleOidException(clientNotRegisteredException);

        assertAll(() -> {
            assertNotNull(result);
            assertThat(result.getStatusCode(), is(HttpStatus.BAD_REQUEST));
            assertThat(result.getBody(), notNullValue());
            assertThat(result.getBody().get("error").asText(), is("invalid_client"));
            assertThat(result.getBody().get("error_description").asText(), is("Client Id not registered: " + clientId));
        });
    }

    @DisplayName("Verify mapping of RequestTooLargeException")
    @Test
    void verifyMappingOfRequestTooLargeException() {
        ParameterTooLongException parameterTooLongException = new ParameterTooLongException("state", 2048);
        var result = webExceptionHandlerUT.handleParameterTooLongException(parameterTooLongException);

        assertAll(() -> {
            assertNotNull(result);
            assertThat(result.getStatusCode(), is(HttpStatus.PAYLOAD_TOO_LARGE));
            assertThat(result.getBody(), notNullValue());
            assertThat(result.getBody().get("error").asText(), is("invalid_request"));
            assertThat(result.getBody().get("error_description").asText(), is("The state parameter exceeds the maximum permitted size of 2048 bytes"));
        });
    }

    @DisplayName("Verify mapping of UseDpopNonceException")
    @Test
    void verifyMappingOfUseDpopNonceException() {
        var nonce = RandomUtil.randomString();
        var useDpopNonceException = new UseDpopNonceException(nonce, "nonononce");
        var result = webExceptionHandlerUT.handleOidException(useDpopNonceException);

        assertAll(() -> {
            assertNotNull(result);
            assertThat(result.getStatusCode(), is(HttpStatus.BAD_REQUEST));
            assertThat(result.getBody(), notNullValue());
            assertThat(result.getBody().get("error").asText(), is("use_dpop_nonce"));
            assertThat(result.getBody().get("error_description").asText(), is("nonononce"));
            assertThat(result.getHeaders().get("DPoP-Nonce"), contains(nonce));
        });
    }

    @DisplayName("Verify mapping of UnauthorizedException")
    @Test
    void verifyMappingOfUnauthorizedException() {
        var unauthorizedException = new UnauthorizedException(TokenType.DPOP.getValue(), "invalid_dpop_proof", "nooo");
        var result = webExceptionHandlerUT.handleUnauthorizedException(unauthorizedException);

        assertAll(() -> {
            assertNotNull(result);
            assertThat(result.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
            assertThat(result.getBody(), nullValue());
            assertThat(result.getHeaders().get(HttpHeaders.WWW_AUTHENTICATE), contains("DPoP realm=\"oid4vci\", error=\"invalid_dpop_proof\", error_description=\"nooo\", algs=" + DPOP_SIGNING_ALGS));
            assertThat(result.getHeaders().get("DPoP-Nonce"), nullValue());
        });
    }

    @DisplayName("Verify mapping of UnauthorizedException caused by UseDpopNonceException")
    @Test
    void verifyMappingOfUnauthorizedUseDpopNonceException() {
        var nonce = RandomUtil.randomString();
        var useDpopNonceException = new UseDpopNonceException(nonce, "nonononce");
        var unauthorizedException = new UnauthorizedException(TokenType.DPOP.getValue(), "invalid_dpop_proof", useDpopNonceException);
        var result = webExceptionHandlerUT.handleUnauthorizedException(unauthorizedException);

        assertAll(() -> {
            assertNotNull(result);
            assertThat(result.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
            assertThat(result.getBody(), nullValue());
            assertThat(result.getHeaders().get(HttpHeaders.WWW_AUTHENTICATE), contains("DPoP realm=\"oid4vci\", error=\"invalid_dpop_proof\", error_description=\"nonononce\", algs=" + DPOP_SIGNING_ALGS));
            assertThat(result.getHeaders().get("DPoP-Nonce"), contains(nonce));
        });
    }

    @DisplayName("Verify mapping of FinishAuthException caused by OidException")
    @Test
    void test001() {
        var oidException = new IdentificationFailedException("user aborted process");
        var finishAuthException = new FinishAuthException(null, null, oidException);

        var result = webExceptionHandlerUT.handleFinishAuthException(finishAuthException);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        Assertions.assertThat(result.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo("http://localhost:8080/?error_description=Identification%20failed&error=access_denied");
    }

    @DisplayName("Verify mapping of FinishAuthException caused by PidServerException")
    @Test
    void test002() {
        var serverException = new PidServerException("too bad", new ClassCastException("foo"));
        var finishAuthException = new FinishAuthException("https://example.com/redirect", "abc321TUV", serverException);

        var result = webExceptionHandlerUT.handleFinishAuthException(finishAuthException);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        Assertions.assertThat(result.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo("https://example.com/redirect?error=server_error&state=abc321TUV");
    }
}
