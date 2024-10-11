/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization;

import de.bdr.openid4vc.vci.data.TokenType;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.testdata.TestConfig;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;

public class ConfigTestData {

    public static final AuthorizationConfiguration AUTH_CONFIG;
    static {
        AUTH_CONFIG = new AuthorizationConfiguration();
        AUTH_CONFIG.setAccessTokenLifetime(Duration.ofSeconds(60));
        AUTH_CONFIG.setAuthorizationCodeLifetime(Duration.ofSeconds(60));
        AUTH_CONFIG.setRequestUriLifetime(Duration.ofSeconds(60));
        AUTH_CONFIG.setDpopNonceLifetime(Duration.ofSeconds(60));
        AUTH_CONFIG.setPidIssuerNonceLifetime(Duration.ofSeconds(60));
        AUTH_CONFIG.setAuthorizationScheme(TokenType.DPOP.getValue());
        AUTH_CONFIG.setMaxPinRetries(3);
        AUTH_CONFIG.setProofTimeTolerance(Duration.ofSeconds(60));
        AUTH_CONFIG.setProofValidity(Duration.ofSeconds(60));
        AUTH_CONFIG.setPinRetryCounterValidity(Duration.ofMinutes(60));
        AUTH_CONFIG.setSessionExpirationTime(Duration.ofMinutes(60));
        try {
            AUTH_CONFIG.setBaseUrl(URI.create(TestConfig.pidiBaseUrl()).toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}