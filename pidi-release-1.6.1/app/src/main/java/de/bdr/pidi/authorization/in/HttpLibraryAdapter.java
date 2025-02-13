/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.in;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialFormat;
import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.service.HttpHeaders;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.domain.PresentationSigningRequest;
import de.bdr.pidi.authorization.core.exception.InvalidCredentialRequestException;
import de.bdr.pidi.authorization.core.exception.InvalidProofException;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.exception.UnsupportedCredentialFormatException;
import de.bdr.pidi.authorization.in.dto.CredentialDto;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialFormat;
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialFormat;
import kotlin.Unit;
import kotlinx.serialization.json.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
public class HttpLibraryAdapter {

    private final Pattern safe = Pattern.compile("[a-zA-Z0-9_.\\-]+");
    private final ObjectMapper mapper = new ObjectMapper();

    private final String authority;

    @SuppressWarnings("java:S1481") // own CredentialFormats needs to be registered in CredentialFormatRegistry
    public HttpLibraryAdapter(AuthorizationConfiguration authorizationConfiguration) {
        authority = authorizationConfiguration.getBaseUrl().toString();
        var mdocFormat = MsoMdocAuthChannelCredentialFormat.INSTANCE;
        var sdJwtFormat = SdJwtVcAuthChannelCredentialFormat.INSTANCE;
    }

    // would be better to return HttpRequest<java.lang.Void>(body=null), but body is declared as NotNull
    public HttpRequest<Unit> getLibraryHttpRequest(HttpMethod method, MultiValueMap<String, String> allHeaders,
                                                   MultiValueMap<String, String> allParams) {
        var uri = ServletUriComponentsBuilder.fromCurrentRequest().build();
        HttpHeaders headers = new HttpHeaders(allHeaders);
        Map<String, String> params = readParams(allParams);

        return HttpRequest.Companion.bodyless(
                method.toString(),
                uri.toUriString(),
                Objects.requireNonNullElse(uri.getPath(), ""),
                headers,
                params);
    }

    public String validateCredentialRequest(String body) {
        var dto = CredentialDto.readFromJson(mapper, body);
        dto.validate(authority);
        return dto.format();
    }

    /**
     * On flow variant B and B' with sdJwt the official, external credential format is "vc+sd-jwt".
     * The format must be replaced to make it internally unique.
     */
    private String parseAndReplaceCredentialFormat(FlowVariant flowVariant, String body) {
        String requestedFormat = validateCredentialRequest(body);
        if (Objects.equals(SdJwtVcCredentialFormat.INSTANCE.getFormat(), requestedFormat)
                && (FlowVariant.B == flowVariant || FlowVariant.B1 == flowVariant)) {
            return body.replace(requestedFormat, SdJwtVcAuthChannelCredentialFormat.INSTANCE.getFormat());
        } else {
            return body;
        }
    }

    public HttpRequest<CredentialRequest> getLibraryCredentialRequest(FlowVariant flowVariant, HttpMethod method, MultiValueMap<String, String> allHeaders,
                                                                      MultiValueMap<String, String> allParams, String body) {
        var uri = ServletUriComponentsBuilder.fromCurrentRequest().build();
        HttpHeaders headers = new HttpHeaders(allHeaders);
        Map<String, String> params = readParams(allParams);

        try {
            var requestBody = parseAndReplaceCredentialFormat(flowVariant, body);
            CredentialRequest credentialRequest = Json.Default.decodeFromString(CredentialRequest.Companion.serializer(), requestBody);
            return new HttpRequest<>(method.toString(),
                    uri.toUriString(),
                    Objects.requireNonNullElse(uri.getPath(), ""),
                    headers,
                    params, credentialRequest);
        } catch (SpecificIllegalArgumentException siae) {
            switch (siae.getReason()) {
                case INVALID_PROOF -> throw new InvalidProofException("Proof JWT could not be parsed", siae);
                case INVALID_CREDENTIAL_FORMAT -> throw new UnsupportedCredentialFormatException(siae.getMessage(), siae);
                default -> throw new InvalidRequestException(siae.getMessage(), siae);
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialRequestException(e.getMessage(), e);
        }
    }

    public HttpRequest<PresentationSigningRequest> getPresentationSigningRequest(HttpMethod method, MultiValueMap<String, String> allHeaders,
                                                                                 MultiValueMap<String, String> allParams, String body) {
        return getRequest(PresentationSigningRequest.class, method, allHeaders, allParams, body);
    }

    private <T> HttpRequest<T> getRequest(Class<T> type, HttpMethod method, MultiValueMap<String, String> allHeaders,
                                                 MultiValueMap<String, String> allParams, String body) {
        var uri = ServletUriComponentsBuilder.fromCurrentRequest().build();
        var headers = new HttpHeaders(allHeaders);
        var params = readParams(allParams);
        final T requestBody;
        try {
            if (StringUtils.isBlank(body)) {
                requestBody = createEmptyRequest(type);
            } else {
                requestBody = mapper.readValue(body, type);
            }
        } catch (JsonProcessingException e) {
            throw new InvalidRequestException(e.getMessage(), e);
        }
        return new HttpRequest<>(method.toString(),
                uri.toUriString(),
                Objects.requireNonNullElse(uri.getPath(), ""),
                headers,
                params, requestBody);
    }

    private static <T> T createEmptyRequest(Class<T> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalAccessError("No empty constructor in " + type.getName() + " defined: " + e.getMessage());
        }
    }

    private Map<String, String> readParams(MultiValueMap<String, String> allParams) {
        Map<String, String> params = new HashMap<>();
        Set<String> multiValued = new HashSet<>();
        for (var entry : allParams.entrySet()) {
            List<String> values = entry.getValue();
            var name = entry.getKey();
            switch (values.size()) {
                case 0:
                    log.warn("no value for param {}", safeName(name));
                    break;
                case 1:
                    params.put(name, values.getFirst());
                    break;
                default:
                    log.warn("multiple values for param {}", safeName(name));
                    multiValued.add(name);
                    break;
            }
        }
        if (!multiValued.isEmpty()) {
            var logMessage = "multi valued parameters: " +
                    String.join(",", multiValued);
            throw new InvalidRequestException("parameters are present more than once", logMessage);
        }
        return params;
    }

    CharSequence safeName(CharSequence input) {
        var matcher = safe.matcher(input);
        if (matcher.matches()) {
            return matcher.group();
        } else {
            return "/unsafe/";
        }
    }

}
