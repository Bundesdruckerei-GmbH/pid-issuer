/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.in.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bdr.pidi.authorization.core.exception.InvalidCredentialRequestException;
import de.bdr.pidi.authorization.core.exception.OIDException;
import de.bdr.pidi.authorization.core.exception.UnsupportedCredentialFormatException;
import de.bdr.pidi.authorization.core.exception.UnsupportedCredentialTypeException;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static de.bdr.pidi.authorization.in.dto.CredentialDto.SupportedFormats.MSO_MDOC;
import static de.bdr.pidi.authorization.in.dto.CredentialDto.SupportedFormats.MSO_MDOC_AUTH_CHANNEL;
import static de.bdr.pidi.authorization.in.dto.CredentialDto.SupportedFormats.SD_JWT;
import static de.bdr.pidi.authorization.in.dto.CredentialDto.SupportedFormats.SD_JWT_AUTH_CHANNEL;

class CredentialDtoTest {
    static Stream<Arguments> validCredentials() {
        return Stream.of(
                Arguments.arguments(SD_JWT.getFormatValue(), "vct", "http://localhost/", SD_JWT.getTypeValue()),
                Arguments.arguments(SD_JWT_AUTH_CHANNEL.getFormatValue(), "vct", "http://localhost/", SD_JWT_AUTH_CHANNEL.getTypeValue()),
                Arguments.arguments(MSO_MDOC.getFormatValue(), "doctype", "", MSO_MDOC.getTypeValue()),
                Arguments.arguments(MSO_MDOC_AUTH_CHANNEL.getFormatValue(), "doctype", "", MSO_MDOC_AUTH_CHANNEL.getTypeValue())
        );
    }

    static Stream<Arguments> invalidTypes() {
        return Stream.of(
                Arguments.arguments(SD_JWT.getFormatValue(), "vct", "http://localhost/", "invalid"),
                Arguments.arguments(SD_JWT_AUTH_CHANNEL.getFormatValue(), "vct", "http://localhost/", "invalid"),
                Arguments.arguments(MSO_MDOC.getFormatValue(), "doctype", "", "invalid"),
                Arguments.arguments(MSO_MDOC_AUTH_CHANNEL.getFormatValue(), "doctype", "", "invalid")
        );
    }

    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("validCredentials")
    void testValidParams(String formatValue, String typeName, String authority, String typeValue) {
        String json = """
                {"format": "%s",
                "%s": "%s"
                }""".formatted(formatValue, typeName, authority + typeValue);
        var dto = CredentialDto.readFromJson(mapper, json);
        Assertions.assertThatNoException().isThrownBy(() -> dto.validate(authority));
    }

    @ParameterizedTest
    @MethodSource("invalidTypes")
    void testInvalidTypes(String formatValue, String typeName, String authority, String typeValue) {
        String json = """
                {"format": "%s",
                "%s": "%s"
                }""".formatted(formatValue, typeName, authority + typeValue);
        var dto = CredentialDto.readFromJson(mapper, json);
        Assertions.assertThatThrownBy(() -> dto.validate(authority))
                .hasMessage("Credential type \"%s\" not supported".formatted(authority + typeValue))
                .asInstanceOf(InstanceOfAssertFactories.type(UnsupportedCredentialTypeException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("unsupported_credential_type");
    }

    @ParameterizedTest
    @EnumSource(value = CredentialDto.SupportedFormats.class, names = "SEED_PID", mode = EnumSource.Mode.EXCLUDE)
    void testMissingType(CredentialDto.SupportedFormats format) {
        String jsonMissingType = """
                {"format": "%s"
                }""".formatted(format.getFormatValue());
        var dto = CredentialDto.readFromJson(mapper, jsonMissingType);
        Assertions.assertThatThrownBy(() -> dto.validate(""))
                .hasMessageContaining(" parameter is missing")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidCredentialRequestException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_credential_request");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"format\": \"\"}",
            "{\"format\": null}"
    })
    void testEmptyFormat(String json) {
        var dto = CredentialDto.readFromJson(mapper, json);
        Assertions.assertThatThrownBy(() -> dto.validate(""))
                .hasMessage("format parameter is missing")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidCredentialRequestException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_credential_request");
    }

    @Test
    void testInvalidFormat() {
        var dto = CredentialDto.readFromJson(mapper, "{\"format\": \"invalid\"}");
        Assertions.assertThatThrownBy(() -> dto.validate(""))
                .hasMessage("Credential format \"invalid\" not supported")
                .asInstanceOf(InstanceOfAssertFactories.type(UnsupportedCredentialFormatException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("unsupported_credential_format");
    }
}