/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance;

import de.bdr.pidi.issuance.core.IssuanceConfiguration;
import de.bdr.pidi.testdata.TestConfig;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;

public class ConfigTestData {
    public static final IssuanceConfiguration ISSUANCE_CONFIG;

    static {
        ISSUANCE_CONFIG = new IssuanceConfiguration();
        ISSUANCE_CONFIG.setLifetime(Duration.ofDays(14L));
        ISSUANCE_CONFIG.setSignerPath("src/test/resources/keystore/issuance-test-keystore.p12");
        ISSUANCE_CONFIG.setSignerPassword("issuance-test");
        ISSUANCE_CONFIG.setSignerAlias("1");
        try {
            ISSUANCE_CONFIG.setBaseUrl(URI.create(TestConfig.pidiBaseUrl()).toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
