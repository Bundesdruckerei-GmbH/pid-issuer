/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.in.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialFormat;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialFormat;
import de.bdr.pidi.authorization.core.exception.InvalidCredentialRequestException;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.exception.UnsupportedCredentialFormatException;
import de.bdr.pidi.authorization.core.exception.UnsupportedCredentialTypeException;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialFormat;
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialFormat;
import de.bdr.pidi.base.requests.SeedCredentialFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialDto(String format, @JsonProperty("doctype") String docType, @JsonProperty("vct") String sdJwt) {
    public static CredentialDto readFromJson(ObjectMapper mapper, String json) {
        try {
            return mapper.readValue(json, CredentialDto.class);
        } catch (JsonProcessingException e) {
            throw new InvalidRequestException(e.getMessage(), e);
        }
    }

    public void validate() {
        if (StringUtils.isBlank(format)) {
            throw new InvalidCredentialRequestException("format parameter is missing");
        }
        var optionalFormat = SupportedFormats.getSupportedFormat(format);
        if (optionalFormat.isEmpty()) {
            throw new UnsupportedCredentialFormatException("Credential format \"%s\" not supported".formatted(format));
        }
        switch (optionalFormat.get()) {
            case SD_JWT -> validateType(SupportedFormats.SD_JWT, "vct", sdJwt);
            case SD_JWT_AUTH_CHANNEL -> validateType(SupportedFormats.SD_JWT_AUTH_CHANNEL, "vct", sdJwt);
            case MSO_MDOC -> validateType(SupportedFormats.MSO_MDOC, "doctype", docType);
            case MSO_MDOC_AUTH_CHANNEL -> validateType(SupportedFormats.MSO_MDOC_AUTH_CHANNEL, "doctype", docType);
            case SEED_PID -> {
                // no further validation
            }
        }
    }

    private void validateType(SupportedFormats supportedFormat, String typeName, String typeValue) {
        if (StringUtils.isBlank(typeValue)) {
            throw new InvalidCredentialRequestException(typeName + " parameter is missing");
        } else if (!supportedFormat.getTypeValue().equals(typeValue)) {
            throw new UnsupportedCredentialTypeException("Credential type \"%s\" not supported".formatted(typeValue));
        }
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum SupportedFormats {
        SD_JWT(SdJwtVcCredentialFormat.INSTANCE.getFormat(), "https://example.bmi.bund.de/credential/pid/1.0"),
        SD_JWT_AUTH_CHANNEL(SdJwtVcAuthChannelCredentialFormat.INSTANCE.getFormat(), "https://example.bmi.bund.de/credential/pid/1.0"),
        MSO_MDOC(MsoMdocCredentialFormat.INSTANCE.getFormat(), "eu.europa.ec.eudi.pid.1"),
        MSO_MDOC_AUTH_CHANNEL(MsoMdocAuthChannelCredentialFormat.INSTANCE.getFormat(), "eu.europa.ec.eudi.pid.1"),
        SEED_PID(SeedCredentialFormat.INSTANCE.getFormat(), "");

        private final String formatValue;
        private final String typeValue;

        public static Optional<SupportedFormats> getSupportedFormat(String formatValue) {
            for (SupportedFormats format : SupportedFormats.values()) {
                if (format.formatValue.equals(formatValue)) {
                    return Optional.of(format);
                }
            }
            return Optional.empty();
        }
    }
}
