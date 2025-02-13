/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.configuration;

import de.bdr.revocation.identification.core.SamlEndpointConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MultiSamlConfigurationFactory {

    private final AutentConfigurationImpl autentConfig;

    @Bean(name = "samlConfig")
    public MultiSamlConfiguration samlConfiguration(@Qualifier("revocationConfiguration") SamlEndpointConfig samlEndpoint) {
        MultiSamlConfigurationImpl multiSamlConfiguration = new MultiSamlConfigurationImpl(autentConfig, samlEndpoint.createSamlConsumerUrl());
        multiSamlConfiguration.initConfigurations();
        return multiSamlConfiguration;
    }
}
