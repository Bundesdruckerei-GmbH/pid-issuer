/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.walletattestation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Setter
@Getter
@Validated
@Configuration
@ConfigurationProperties(prefix = "pidi.attestation")
public class AttestationConfiguration {
    private Duration proofTimeTolerance;
    private Duration proofValidity;
}
