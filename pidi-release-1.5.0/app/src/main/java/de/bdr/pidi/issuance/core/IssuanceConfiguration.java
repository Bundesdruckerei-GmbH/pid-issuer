/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.core;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.net.URL;
import java.time.Duration;

@Setter
@Getter
@Validated
@Configuration
@ConfigurationProperties(prefix = "pidi.issuance")
public class IssuanceConfiguration {
    /**
     * the base url of the pid-issuer
     */
    @NotNull
    private URL baseUrl;
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
