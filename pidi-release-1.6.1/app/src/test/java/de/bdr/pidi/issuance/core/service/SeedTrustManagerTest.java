/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */

package de.bdr.pidi.issuance.core.service;

import com.nimbusds.jose.JWSAlgorithm;
import de.bdr.pidi.authorization.out.issuance.SeedException;
import de.bdr.pidi.base.FileResourceHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SeedTrustManagerTest {

    static SeedTrustManager out;
    static String signatureAlias = null;
    static String encryptionAlias = null;

    @BeforeAll
    static void setupClass() throws IOException {
        Properties prop = new Properties();
        try (var input = SeedTrustManager.class.getResourceAsStream("/application.properties")) {
            prop.load(input);
        }
        encryptionAlias = prop.getProperty("pidi.issuance.seed-enc-alias");
        signatureAlias = prop.getProperty("pidi.issuance.seed-sig-alias");
        out = new SeedTrustManager(prop.getProperty("pidi.issuance.seed-path"),
                prop.getProperty("pidi.issuance.seed-password"),
                encryptionAlias, signatureAlias,
                new FileResourceHelper());
    }

    @Test
    void when_currentSigner_then_ok() {
        var result = out.currentSigner();

        assertNotNull(result);
        assertEquals(signatureAlias, result.keyIdentifier());
        assertTrue(result.signer().supportedJWSAlgorithms().contains(JWSAlgorithm.ES256));
    }

    @Test
    void when_currentEncrypter_then_ok() {
        var result = out.currentEncrypter();

        assertNotNull(result);
        assertEquals(encryptionAlias, result.keyIdentifier());
        assertEquals("AES", result.key().getAlgorithm());
    }

    @Test
    void given_currentKeyId_when_verifierForKeyId_then_ok() {
        var result = out.verifierForKeyId(signatureAlias);

        assertNotNull(result);
    }

    @Test
    void given_unknownKeyId_when_verifierForKeyId_then_fail() {
        assertThrows(SeedException.class, () -> out.verifierForKeyId("__unknown__"));
    }

    @Test
    void given_unknownKeyId_when_encryptionKeyForKeyId_then_fail() {
        assertThrows(SeedException.class, () -> out.encryptionKeyForKeyId("__unknown__"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "seed-enc_2024.001", "Id23b", "R2D2"})
    void given_goodValues_when_safeKeyId_then_ok(String keyId) {
        var result = out.safeKeyId(keyId);
        assertEquals(keyId, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "a+b", "foo/bar", "abc=", "r?t2", ",bar"})
    void given_badValues_when_safeKeyId_then_fail(String keyId) {
        assertThrows(SeedException.class, () -> out.safeKeyId(keyId));
    }

}
