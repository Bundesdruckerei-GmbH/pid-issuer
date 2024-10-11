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
import java.util.Map;
import java.util.stream.Stream;

import static java.time.Instant.now;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ClientAttestationJwtTest {

    public static Stream<Arguments> invalidClientAttestationJwtParameter() {
        return Stream.of(
                arguments(null, "subject", now(), "cnf", Map.of("jwk", "value"), "iss claim is missing"),
                arguments(" ", "subject", now(), "cnf", Map.of("jwk", "value"), "iss claim is missing"),
                arguments("issuer", null, now(), "cnf", Map.of("jwk", "value"), "sub claim is missing"),
                arguments("issuer", " ", now(), "cnf", Map.of("jwk", "value"), "sub claim is missing"),
                arguments("issuer", "subject", null, "cnf", Map.of("jwk", "value"), "exp claim is missing"),
                arguments("issuer", "subject", now(), "cnf", Map.of("badKey", "value"), "cnf.jwk value is invalid"),
                arguments("issuer", "subject", now(), "badClaim", Map.of("jwk", "value"), "cnf claim is missing")
        );
    }

    @DisplayName("Verify invalid ClientAttestationJwt parameter")
    @MethodSource("invalidClientAttestationJwtParameter")
    @ParameterizedTest
    void test001(String issuer, String subject, Instant expirationTime, String claimName, Object claimValue, String errorMessage) {
        SignedJWT signedJWT = TestUtils.getClientAttestationJwt(issuer, subject, expirationTime, now(), now(), claimName, claimValue);
        var exception = assertThrows(IllegalArgumentException.class, () -> ClientAttestationJwt.from(signedJWT));
        assertThat(exception.getMessage(), is(errorMessage));
    }
}