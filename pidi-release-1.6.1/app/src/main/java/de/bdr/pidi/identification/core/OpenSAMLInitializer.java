/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core;

import de.bdr.pidi.identification.core.configuration.SamlEndpointConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OpenSAMLInitializer {

    private final EidAuth eidAuth;
    private final SamlEndpointConfig samlEndpointConfig;

    public OpenSAMLInitializer(EidAuth eidAuth,
                               @Qualifier("infoV2") SamlEndpointConfig samlEndpointConfig) {
        // it is not important which version to choose here
        this.eidAuth = eidAuth;
        this.samlEndpointConfig = samlEndpointConfig;
    }

    @PostConstruct
    void init() {
        log.debug("Calling createSamlRedirectBindingUrl");
        eidAuth.createSamlRedirectBindingUrl("abc", samlEndpointConfig.createSamlConsumerUrl());
        log.debug("Done with createSamlRedirectBindingUrl");
    }
}
