/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.walletattestation;

import com.nimbusds.jwt.SignedJWT;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.time.Instant.now;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ClientAttestationPopJwtTest {
    public static Stream<Arguments> invalidClientAttestationPopJwtParameter() {
        return Stream.of(
                arguments(null, now(), List.of("audience"), "jwtId", "iss claim is missing"),
                arguments(" ", now(), List.of("audience"), "jwtId", "iss claim is missing"),
                arguments("issuer", null, List.of("audience"), "jwtId", "exp claim is missing"),
                arguments("issuer", now(), null, "jwtId", "aud claim is missing"),
                arguments("issuer", now(), Collections.emptyList(), null, "jti claim is missing"),
                arguments("issuer", now(), List.of("audience"), " ", "jti claim is missing")
        );
    }

    @DisplayName("Verify invalid ClientAttestationPopJwt parameter")
    @MethodSource("invalidClientAttestationPopJwtParameter")
    @ParameterizedTest
    void test001(String issuer, Instant expirationTime, List<String> audience, String jwtId, String errorMessage) {
        SignedJWT signedJWT = TestUtils.getClientAttestationPopJwt(issuer, expirationTime, now(), now(), audience, jwtId);
        var exception = assertThrows(IllegalArgumentException.class, () -> ClientAttestationPopJwt.from(signedJWT));
        assertThat(exception.getMessage(), is(errorMessage));
    }

}