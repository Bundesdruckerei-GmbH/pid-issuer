/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.core;

import de.bdr.pidi.base.BaseUrlConfiguration;
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
@ConfigurationProperties(prefix = "pidi.issuance")
public class IssuanceConfiguration extends BaseUrlConfiguration {
    private String signerPath;
    private String signerPassword;
    private String signerAlias;
    private String seedPath;
    private String seedPassword;
    private String seedEncAlias;
    private String seedSigAlias;
    private Duration lifetime;
    private Duration seedValidity;
}
