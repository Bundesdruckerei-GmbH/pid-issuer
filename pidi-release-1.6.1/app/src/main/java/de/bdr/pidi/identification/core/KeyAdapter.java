/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core;

import de.bdr.pidi.identification.core.exception.CryptoConfigException;
import de.governikus.panstar.sdk.saml.configuration.SamlKeyMaterial;
import de.governikus.panstar.sdk.utils.crypto.KeystoreLoader;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

public class KeyAdapter implements SamlKeyMaterial {

    private final X509Certificate requestEncryptionCert;

    private final X509Certificate signatureValidationCert;

    private final PrivateKey samlRequestSigningPrivateKey;
    private final KeyPair samlResponseDecryptionKeyPair;

    public KeyAdapter(X509Certificate requestEncryptionCert,
                      X509Certificate signatureValidationCert,
                      KeyParams requestSignatureKeyParams,
                      KeyParams responseDecryptKeyParams) {
        this.requestEncryptionCert = requestEncryptionCert;
        this.signatureValidationCert = signatureValidationCert;
        this.samlRequestSigningPrivateKey = loadSamlRequestSigningPrivateKey(requestSignatureKeyParams);
        this.samlResponseDecryptionKeyPair = loadSamlResponseDecryptionKeyPair(responseDecryptKeyParams);
    }

    private static PrivateKey loadSamlRequestSigningPrivateKey(KeyParams keyParams) {
        try {
            PrivateKey privateKey = (PrivateKey) keyParams.getKey();
            if (privateKey == null) {
                throw new CryptoConfigException("failed to access our signature key");
            }
            return privateKey;
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new CryptoConfigException("failed to access our signature key", e);
        }
    }

    private static KeyPair loadSamlResponseDecryptionKeyPair(KeyParams keyParams) {
        return KeystoreLoader
                .loadKeyPairFromKeyStore(keyParams.keyStore, keyParams.alias, keyParams.password)
                .orElseThrow(() -> new CryptoConfigException("failed to access our decryption key pair"));
    }

    @Override
    public PrivateKey getSamlRequestSigningPrivateKey() {
        return samlRequestSigningPrivateKey;
    }

    @Override
    public KeyPair getSamlResponseDecryptionKeyPair() {
        return samlResponseDecryptionKeyPair;
    }

    @Override
    public X509Certificate getSamlResponseSignatureValidatingCertificate() {
        return this.signatureValidationCert;
    }

    @Override
    public X509Certificate getSamlRequestEncryptionCertificate() {
        return this.requestEncryptionCert;
    }

    public record KeyParams(KeyStore keyStore, String alias, String password) {
        public Key getKey() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
            return keyStore.getKey(alias, password.toCharArray());
        }
    }
}
