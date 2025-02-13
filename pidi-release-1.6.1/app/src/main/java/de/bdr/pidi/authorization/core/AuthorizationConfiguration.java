/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.base.BaseUrlConfiguration;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "pidi.authorization")
public class AuthorizationConfiguration extends BaseUrlConfiguration {

    /**
     * the lifetime of a request uri
     */
    private Duration requestUriLifetime;

    /**
     * the lifetime of an access token
     */
    private Duration accessTokenLifetime;

    /**
     * lifetime of a dpop-nonce
     */
    private Duration dpopNonceLifetime;

    /**
     * lifetime of an authorization code
     */
    private Duration authorizationCodeLifetime;

    private Duration proofTimeTolerance;

    private Duration proofValidity;

    private String authorizationScheme;

    private Duration sessionExpirationTime;

    /**
     * lifetime of a pid-issuer-nonce
     */
    private Duration pidIssuerNonceLifetime;

    /**
     * lifetime of a pin-retry-counter
     */
    private Duration pinRetryCounterValidity;

    /**
     * maximum pin retries
     */
    private int maxPinRetries;

    private int batchIssuanceMaxSize;

    public String getCredentialIssuerIdentifier(@NotNull FlowVariant flowVariant) {
        return getBaseUrl() + flowVariant.urlPath;
    }
}
