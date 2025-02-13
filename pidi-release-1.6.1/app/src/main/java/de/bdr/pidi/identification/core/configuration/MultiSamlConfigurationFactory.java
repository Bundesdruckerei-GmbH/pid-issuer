/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MultiSamlConfigurationFactory {

    private final AutentConfigurationImpl autentConfig;

    @Bean(name = "samlConfigApiV1")
    public MultiSamlConfiguration samlConfigurationV1(@Qualifier("infoV1") SamlEndpointConfig samlEndpoint) {
        return new MultiSamlConfigurationImpl(autentConfig, samlEndpoint.createSamlConsumerUrl());
    }

    @Bean(name = "samlConfigApiV2")
    public MultiSamlConfiguration samlConfigurationV2(@Qualifier("infoV2") SamlEndpointConfig samlEndpoint) {
        return new MultiSamlConfigurationImpl(autentConfig, samlEndpoint.createSamlConsumerUrl());
    }

}
