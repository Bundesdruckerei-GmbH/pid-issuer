/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
