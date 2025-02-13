/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.config;

import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Validated
@Component
@ConfigurationProperties(prefix = "revocation.identification")
public class IdentificationConfiguration {

    private String loggedInPath;
    private String tcTokenPath;
    private String samlConsumerPath;

    /**
     * Initial session duration before authentication
     */
    private Duration initialSessionDuration;
    /**
     * Minimum session duration for an authenticated session
     */
    private Duration minAuthenticatedSessionDuration;
    /**
     * The session can be extended for the minimum session duration until the maximum session duration is reached
     */
    private Duration maxAuthenticatedSessionDuration;
    /**
     * Base URL of Info Service
     */
    private String baseUrl;
    /**
     * Base path for Info Service API
     */
    private String basePath;
    /**
     * External URL of Info Frontend
     */
    private String frontendUrl;
    /**
     * Enable logging of the pseudonym when authentication via eID
     */
    private boolean dumpPseudonym;

    @NestedConfigurationProperty
    private Server server;

    /**
     * This value holds the name of the service provider for the Autent server.
     *
     * @see "classpath:saml-sdk.properties"
     */
    private String serviceProviderName;

    /**
     * This value holds the values of the keystore that should be used for the XML signature.
     */
    @NestedConfigurationProperty
    private KeyStoreInfo xmlsigKeystore;

    /**
     * This value holds the values of the keystore that should be used for the XML encryption.
     */
    @NestedConfigurationProperty
    private KeyStoreInfo xmlencKeystore;

    public boolean isLoggingPseudonymsAllowed() {
        return dumpPseudonym;
    }

    @Setter
    @Getter
    public static class Server {
        /**
         * This value holds the URL where the Autent server receives SAML requests.
         *
         * @see "classpath:saml-sdk.properties"
         */
        private String url;

        /**
         * This value holds the path to the signature certificate of the Autent server.
         *
         * @see "classpath:saml-sdk.properties"
         */
        private List<String> certificateSigPaths;

        /**
         * This value holds the path to the encryption certificate of the Autent server.
         *
         * @see "classpath:saml-sdk.properties"
         */
        private String certificateEncPath;

        public Server(String url, List<String> certificateSigPaths, String certificateEncPath) {
            this.url = url;
            this.certificateSigPaths = certificateSigPaths;
            this.certificateEncPath = certificateEncPath;
        }

    }

    @Setter
    @Getter
    public static class KeyStoreInfo {
        /**
         * This value holds the path to the keystore.
         *
         * @see "classpath:saml-sdk.properties"
         */
        private String path;

        /**
         * This value holds the alias of the key.
         *
         * @see "classpath:saml-sdk.properties"
         */
        private String alias;

        /**
         * This value holds the password to access the keystore.
         *
         * @see "classpath:saml-sdk.properties"
         */
        private String password;

        /**
         * This value holds the password to access the key for alias {@link #alias}.
         *
         * @see "classpath:saml-sdk.properties"
         */
        private String keyPassword;

        public KeyStoreInfo(String path, String alias, String password) {
            this.path = path;
            this.alias = alias;
            this.password = password;
            this.keyPassword = password;
        }

    }
}
