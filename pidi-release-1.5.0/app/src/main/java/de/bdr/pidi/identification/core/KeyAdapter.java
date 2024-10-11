/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core;

import de.bdr.pidi.identification.core.exception.CryptoConfigException;
import de.governikus.panstar.sdk.saml.configuration.SamlKeyMaterial;
import de.governikus.panstar.sdk.utils.crypto.KeystoreLoader;

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

    private final KeyStore requestSignatureStore;
    private final String requestSignatureAlias;
    private final String requestSignatureKeyPassword;
    private final KeyStore responseDecryptStore;
    private final String responseDecryptAlias;
    private final String responseDecryptPassword;
    private final PrivateKey samlRequestSigningPrivateKey;
    private final KeyPair samlResponseDecryptionKeyPair;

    public KeyAdapter(X509Certificate requestEncryptionCert,
                      X509Certificate signatureValidationCert,
                      KeyStore requestSignatureStore,
                      String requestSignatureAlias,
                      String requestSignatureKeyPassword,
                      KeyStore responseDecryptStore,
                      String responseDecryptAlias,
                      String responseDecryptPassword) {
        this.requestEncryptionCert = requestEncryptionCert;
        this.signatureValidationCert = signatureValidationCert;
        this.requestSignatureStore = requestSignatureStore;
        this.requestSignatureAlias = requestSignatureAlias;
        this.requestSignatureKeyPassword = requestSignatureKeyPassword;
        this.responseDecryptStore = responseDecryptStore;
        this.responseDecryptAlias = responseDecryptAlias;
        this.responseDecryptPassword = responseDecryptPassword;
        this.samlRequestSigningPrivateKey = loadSamlRequestSigningPrivateKey();
        this.samlResponseDecryptionKeyPair = loadSamlResponseDecryptionKeyPair();
    }
    @Override
    public PrivateKey getSamlRequestSigningPrivateKey() {
        return samlRequestSigningPrivateKey;
    }

    private PrivateKey loadSamlRequestSigningPrivateKey() {
        try {
            PrivateKey privateKey = (PrivateKey) this.requestSignatureStore.getKey(this.requestSignatureAlias, this.requestSignatureKeyPassword.toCharArray());
            if (privateKey == null) {
                throw new CryptoConfigException("failed to access our signature key");
            }
            return privateKey;
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new CryptoConfigException("failed to access our signature key", e);
        }
    }

    @Override
    public KeyPair getSamlResponseDecryptionKeyPair() {
        return samlResponseDecryptionKeyPair;
    }

    private KeyPair loadSamlResponseDecryptionKeyPair() {
        return KeystoreLoader
                .loadKeyPairFromKeyStore(this.responseDecryptStore, this.responseDecryptAlias,
                        this.responseDecryptPassword)
                .orElseThrow(() -> new CryptoConfigException("failed to access our decryption key pair"));
    }

    @Override
    public X509Certificate getSamlResponseSignatureValidatingCertificate() {
        return this.signatureValidationCert;
    }

    @Override
    public X509Certificate getSamlRequestEncryptionCertificate() {
        return this.requestEncryptionCert;
    }
}
