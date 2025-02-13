/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.clientconfiguration.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.UUID;

@Setter
@Getter
@Validated
@Configuration
@ConfigurationProperties(prefix = "pidi.client")
public class ClientConfiguration {
    private Map<UUID, String> clientCert;
}
