/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core.configuration;

import de.bdr.pidi.identification.config.IdentificationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EndpointConfiguration {

    @Bean(name="infoV1")
    @SuppressWarnings("java:S1075") // URLs are not configurable, so they should not be part of application config
    public InfoConfiguration infoV1Configuration(IdentificationConfiguration identificationConfiguration) {
        return new InfoConfiguration(identificationConfiguration, "/eid/v1/tcToken", "/eid/v1/saml-consumer");
    }

    @Bean(name="infoV2")
    @SuppressWarnings("java:S1075") // URLs are not configurable, so they should not be part of application config
    public InfoConfiguration infoV2Configuration(IdentificationConfiguration identificationConfiguration) {
        return new InfoConfiguration(identificationConfiguration, "/eid/v1/tcToken", "/eid/v1/saml-consumer");
    }

}
