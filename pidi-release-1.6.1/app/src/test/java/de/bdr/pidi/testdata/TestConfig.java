/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.testdata;

import java.util.function.Supplier;

public class TestConfig {

    public static final String DEV = "pidi-dev";

    public static final String TEST = "pidi-test";

    public static final String DEMO = "pidi-demo";

    public static final String PLAIN_LOCAL = "plain";

    public static String getTestConfig() {
        return System.getProperty("TEST_CONFIG", PLAIN_LOCAL);
    }

    public static String pidiBaseUrl() {
        return getProperty(
                () -> "https://pidi.pidi.dev.qa.ext.csp.bop",
                () -> "https://pidi.pidi.test.qa.ext.csp.bop",
                () -> "https://demo.pid-issuer.bundesdruckerei.de",
                () -> "http://pidi.localhost.bdr.de:8080"
        );
    }

    public static String pidiHostnameFromMock() {
        return getProperty(
                () -> "https://pidi.pidi.dev.qa.ext.csp.bop",
                () -> "https://pidi.pidi.test.qa.ext.csp.bop",
                () -> "https://demo.pid-issuer.bundesdruckerei.de",
                () -> "http://pidi.localhost.bdr.de:8080"
        );
    }

    public static String getEidMockHostname() {
        return System.getProperty("EIDMOCKHOSTNAME", "localhost");
    }

    public static int getEidMockPort() {
        return 24727;
    }

    private static <T> T getProperty(Supplier<T> pidiDev, Supplier<T> pidiTest, Supplier<T> pidiDemo, Supplier<T> plainLocal) {
        String testConfig = getTestConfig();
        return switch (testConfig) {
            case DEV -> pidiDev.get();
            case TEST -> pidiTest.get();
            case DEMO -> pidiDemo.get();
            case PLAIN_LOCAL -> plainLocal.get();
            default -> throw new RuntimeException("unknown TEST_CONFIG: " + testConfig);
        };
    }
}
