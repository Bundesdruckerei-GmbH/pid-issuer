/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.configuration;

import de.bdr.revocation.identification.config.IdentificationConfiguration;
import de.bdr.revocation.identification.core.SamlEndpointConfig;
import de.bdr.revocation.identification.core.model.Authentication;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
public class RevocationConfiguration implements SamlEndpointConfig, Tr03124Configuration {

    private static final String AUTH_PARAM = "auth";
    private static final String REF_PARAM = "ref";

    private final IdentificationConfiguration identificationConfiguration;

    protected RevocationConfiguration(IdentificationConfiguration identificationConfiguration) {
        this.identificationConfiguration = identificationConfiguration;
    }

    @Override
    public String createLoggedInUrl(String referenceId) {
        return UriComponentsBuilder.fromUriString(identificationConfiguration.getFrontendUrl())
                .path(identificationConfiguration.getBasePath())
                .path(identificationConfiguration.getLoggedInPath())
                .queryParam(REF_PARAM, referenceId)
                .build().toString();
    }

    @Override
    public String getAuthParam() {
        return AUTH_PARAM;
    }

    @Override
    public String createAuthenticationLink(Authentication auth) {
        var builder = UriComponentsBuilder.fromUriString(identificationConfiguration.getBaseUrl())
                .path(identificationConfiguration.getBasePath())
                .path(identificationConfiguration.getTcTokenPath())
                .queryParam(AUTH_PARAM, auth.getTokenId())
                .build();
        return URLEncoder.encode(builder.toString(), StandardCharsets.US_ASCII);
    }

    @Override
    public String createSamlConsumerUrl() {
        return UriComponentsBuilder.fromUriString(identificationConfiguration.getBaseUrl())
                .path(identificationConfiguration.getBasePath())
                .path(identificationConfiguration.getSamlConsumerPath())
                .build().toString();
    }
}
