/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core.configuration;

import de.bdr.pidi.identification.config.IdentificationConfiguration;
import de.bdr.pidi.identification.core.model.Authentication;
import lombok.Getter;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class InfoConfiguration implements SamlEndpointConfig, Tr03124Configuration {

    private final String loggedInPath;
    private final String errorPath;
    private final String refParam;
    private final String codeParam;
    private final String tcTokenPath;
    private final String samlConsumerPath;
    private final IdentificationConfiguration identificationConfiguration;

    @Getter
    private final String authParam;

    protected InfoConfiguration(IdentificationConfiguration identificationConfiguration, String tcTokenPath, String samlConsumerPath) {
        this(identificationConfiguration, tcTokenPath, samlConsumerPath, "/logged-in", "/error");
    }

    protected InfoConfiguration(IdentificationConfiguration identificationConfiguration, String tcTokenPath, String samlConsumerPath, String loggedInPath, String errorPath) {
        this.loggedInPath = loggedInPath;
        this.errorPath = errorPath;
        this.tcTokenPath = tcTokenPath;
        this.samlConsumerPath = samlConsumerPath;
        this.identificationConfiguration = identificationConfiguration;
        this.refParam = "ref";
        this.codeParam = "code";
        this.authParam = "auth";
    }

    @Override
    public String createFrontendLoggedInUrl(String referenceId, String lang) {
        var builder = UriComponentsBuilder.fromUriString(identificationConfiguration.getFrontendUrl())
                .path(isDefaultLanguage(lang) ? loggedInPath : "/" + lang + loggedInPath)
                .queryParam(refParam, referenceId);

        return builder.build().toString();
    }

    private boolean isDefaultLanguage(String lang) {
        return "de".equals(lang);
    }

    @Override
    public String createFrontendErrorUrl(String code) {
        return UriComponentsBuilder.fromUriString(identificationConfiguration.getFrontendUrl())
                .path(errorPath)
                .queryParam(codeParam, code)
                .build().toString();
    }

    @Override
    public String createLoggedInUrl(String referenceId) {
        return UriComponentsBuilder.fromUriString(identificationConfiguration.getBaseUrl().toString())
                .path(identificationConfiguration.getBasePath())
                .path(loggedInPath)
                .queryParam(refParam, referenceId)
                .build().toString();
    }

    @Override
    public String createAuthenticationLink(Authentication auth) {
        var builder = UriComponentsBuilder.fromUriString(identificationConfiguration.getBaseUrl().toString())
                .path(identificationConfiguration.getBasePath())
                .path(tcTokenPath)
                .queryParam(getAuthParam(), auth.getTokenId())
                .build();
        return URLEncoder.encode(builder.toString(), StandardCharsets.US_ASCII);
    }

    @Override
    public String createSamlConsumerUrl() {
        return UriComponentsBuilder.fromUriString(identificationConfiguration.getBaseUrl().toString())
                .path(identificationConfiguration.getBasePath())
                .path(samlConsumerPath)
                .build().toString();
    }
}
