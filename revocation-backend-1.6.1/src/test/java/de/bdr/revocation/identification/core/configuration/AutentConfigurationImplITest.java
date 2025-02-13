/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.configuration;

import de.bdr.revocation.issuance.IntegrationTest;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class AutentConfigurationImplITest extends IntegrationTest {

    @Autowired
    AutentConfigurationImpl out;

    @Test
    void given_path_signature_certificate_is_properly_located() {
        X509Certificate cert = out.getAutentSamlSignatureCertificate();
        assertThat(cert).isNotNull();
    }

    @Test
    void given_path_encryption_certificate_is_returned() {
        X509Certificate cert = out.getAutentSamlEncryptionCertificate();
        assertThat(cert).isNotNull();
    }

    @Test
    void given_path_decryption_keystore_is_returned() {
        KeyStore store = out.getServiceProviderDecryptionKeystore();
        assertThat(store).isNotNull();
    }

    @Test
    void given_path_signature_keystore_is_returned() {
        KeyStore store = out.getServiceProviderSignatureKeystore();
        assertThat(store).isNotNull();
    }
}