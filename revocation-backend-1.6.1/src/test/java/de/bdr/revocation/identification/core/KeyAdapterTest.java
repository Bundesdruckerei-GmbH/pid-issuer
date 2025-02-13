/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core;

import de.bdr.revocation.identification.core.configuration.FileResourceHelper;
import de.bdr.revocation.identification.core.exception.CryptoConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

class KeyAdapterTest {

    private static X509Certificate requestEncryptionCert;
    private static X509Certificate signatureValidationCert;
    private static KeyStore requestSignatureStore;
    private final String requestSignatureAlias = "sign.localhost";
    private final String requestSignatureKeyPassword = "2382";
    private static KeyStore responseDecryptStore;
    private final String responseDecryptAlias = "encr.localhost";
    private final String responseDecryptPassword = "3613";

    @BeforeAll
    static void loadData() {
        var fileResourceHelper = new FileResourceHelper();

        signatureValidationCert = fileResourceHelper.readCertificate("./src/test/resources/keys/test/demo.governikus-eid.sign.cer");
        requestEncryptionCert = fileResourceHelper.readCertificate("./src/test/resources/keys/test/demo.governikus-eid.encr.cer");
        requestSignatureStore = fileResourceHelper.readKeyStore("./src/test/resources/keys/test/sign.localhost_2382.p12", "2382");
        responseDecryptStore = fileResourceHelper.readKeyStore("./src/test/resources/keys/test/encr.localhost_3613.p12", "3613");
    }

    private KeyAdapter given_correct_key_adapter() {
        var signatureKeyParams = new KeyAdapter.KeyParams(
                requestSignatureStore,
                requestSignatureAlias,
                requestSignatureKeyPassword);
        var decryptionKeyParams = new KeyAdapter.KeyParams(
                responseDecryptStore,
                responseDecryptAlias,
                responseDecryptPassword);
        return new KeyAdapter(
                requestEncryptionCert,
                signatureValidationCert,
                signatureKeyParams,
                decryptionKeyParams);
    }

    private KeyAdapter given_bad_key_adapter_wrong_alias() {
        var signatureKeyParams = new KeyAdapter.KeyParams(
                requestSignatureStore,
                "nothing",
                requestSignatureKeyPassword);
        var decryptionKeyParams = new KeyAdapter.KeyParams(
                responseDecryptStore,
                "neither",
                responseDecryptPassword);
        return new KeyAdapter(
                requestEncryptionCert,
                signatureValidationCert,
                signatureKeyParams,
                decryptionKeyParams);
    }

    private KeyAdapter given_bad_key_adapter_wrong_password() {
        var signatureKeyParams = new KeyAdapter.KeyParams(
                requestSignatureStore,
                requestSignatureAlias,
                "wrong");
        var decryptionKeyParams = new KeyAdapter.KeyParams(
                responseDecryptStore,
                responseDecryptAlias,
                "bad");
        return new KeyAdapter(
                requestEncryptionCert,
                signatureValidationCert,
                signatureKeyParams,
                decryptionKeyParams);
    }

    @Test
    void testOk() {
        KeyAdapter out = given_correct_key_adapter();

        Assertions.assertEquals(requestEncryptionCert, out.getSamlRequestEncryptionCertificate());
        Assertions.assertEquals(signatureValidationCert, out.getSamlResponseSignatureValidatingCertificate());

        Assertions.assertNotNull(out.getSamlRequestSigningPrivateKey());
        Assertions.assertNotNull(out.getSamlResponseDecryptionKeyPair());
    }

    @Test
    void testExceptionHandling() {
        Assertions.assertThrows(CryptoConfigException.class,
                this::given_bad_key_adapter_wrong_alias);
    }

    @Test
    void testExceptionHandling_password() {
        Assertions.assertThrows(CryptoConfigException.class,
                this::given_bad_key_adapter_wrong_password);
    }
}