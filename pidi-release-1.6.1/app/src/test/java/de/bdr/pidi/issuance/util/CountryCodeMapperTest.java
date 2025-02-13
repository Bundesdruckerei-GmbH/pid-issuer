/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.util;

import com.nimbusds.oauth2.sdk.ParseException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CountryCodeMapperTest {

    public static Stream<Arguments> validValues() {
        return Stream.of(
                arguments("D", "DE"),
                arguments("DE", "DE"),
                arguments("DEU", "DE"),
                arguments("VAT", "VA"),
                arguments("ATF", "TF"),
                arguments("EST", "EE"),
                arguments("SWZ", "SZ"),
                arguments(" ", null),
                arguments("", null),
                arguments(null, null)
                );
    }

    @ParameterizedTest
    @MethodSource("validValues")
    void testCountryCodeMapper(String source, String expected) throws ParseException {
        assertEquals(expected, CountryCodeMapper.mapToISO3166_1Alpha2CountryCode(source));
    }

    @ParameterizedTest
    @ValueSource(strings = {"_", "!-", "123", "BQAQ", "AAA", "AA", "Z", "GBD", "GBN", "GBO", "GBS", "GBP", "KS", "RKS", "EU", "EUE"})
    void testCountryCodeMapper(String invalid) {
        assertThrows(ParseException.class, () -> CountryCodeMapper.mapToISO3166_1Alpha2CountryCode(invalid));
    }
}