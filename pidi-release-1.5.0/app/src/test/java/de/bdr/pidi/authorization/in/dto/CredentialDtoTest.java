/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.in.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bdr.pidi.authorization.core.exception.InvalidCredentialRequestException;
import de.bdr.pidi.authorization.core.exception.OIDException;
import de.bdr.pidi.authorization.core.exception.UnsupportedCredentialFormatException;
import de.bdr.pidi.authorization.core.exception.UnsupportedCredentialTypeException;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static de.bdr.pidi.authorization.in.dto.CredentialDto.SupportedFormats.MSO_MDOC;
import static de.bdr.pidi.authorization.in.dto.CredentialDto.SupportedFormats.SD_JWT;

class CredentialDtoTest {
    static Stream<Arguments> validCredentials() {
        return Stream.of(
                Arguments.arguments(SD_JWT.getFormatValue(), "vct", SD_JWT.getTypeValue()),
                Arguments.arguments(MSO_MDOC.getFormatValue(), "doctype", MSO_MDOC.getTypeValue())
        );
    }

    static Stream<Arguments> invalidTypes() {
        return Stream.of(
                Arguments.arguments(SD_JWT.getFormatValue(), "vct", "invalid"),
                Arguments.arguments(MSO_MDOC.getFormatValue(), "doctype", "invalid")
        );
    }

    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("validCredentials")
    void testValidParams(String formatValue, String typeName, String typeValue) {
        String json = """
                {"format": "%s",
                "%s": "%s"
                }""".formatted(formatValue, typeName, typeValue);
        var dto = CredentialDto.readFromJson(mapper, json);
        Assertions.assertThatNoException().isThrownBy(dto::validate);
    }

    @ParameterizedTest
    @MethodSource("invalidTypes")
    void testInvalidTypes(String formatValue, String typeName, String typeValue) {
        String json = """
                {"format": "%s",
                "%s": "%s"
                }""".formatted(formatValue, typeName, typeValue);
        var dto = CredentialDto.readFromJson(mapper, json);
        Assertions.assertThatThrownBy(dto::validate)
                .hasMessage("Credential type \"%s\" not supported".formatted(typeValue))
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
        Assertions.assertThatThrownBy(dto::validate)
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
        Assertions.assertThatThrownBy(dto::validate)
                .hasMessage("format parameter is missing")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidCredentialRequestException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_credential_request");
    }

    @Test
    void testInvalidFormat() {
        var dto = CredentialDto.readFromJson(mapper, "{\"format\": \"invalid\"}");
        Assertions.assertThatThrownBy(dto::validate)
                .hasMessage("Credential format \"invalid\" not supported")
                .asInstanceOf(InstanceOfAssertFactories.type(UnsupportedCredentialFormatException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("unsupported_credential_format");
    }
}