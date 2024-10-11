/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.HasBaseUrl;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.net.URL;
import java.time.Duration;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "pidi.authorization")
public class AuthorizationConfiguration implements HasBaseUrl {

    /**
     * the lifetime of a request uri
     */
    private Duration requestUriLifetime;

    /**
     * the lifetime of an access token
     */
    private Duration accessTokenLifetime;

    /**
     * the base url of the pid-issuer
     */
    @NotNull
    private URL baseUrl;

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

    public String getCredentialIssuerIdentifier(@NotNull FlowVariant flowVariant) {
        var tmp = baseUrl.toString();
        if (tmp.endsWith("/")) {
            return baseUrl + flowVariant.urlPath;
        } else {
            return baseUrl.toString() + '/' + flowVariant.urlPath;
        }
    }
}
