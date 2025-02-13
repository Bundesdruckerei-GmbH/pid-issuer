/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.core.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import de.bdr.pidi.authorization.out.issuance.SeedException;
import de.bdr.pidi.base.FileResourceHelper;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.regex.Pattern;

public class SeedTrustManager {

    private static final Pattern KEY_IDENTIFIER = Pattern.compile("[A-Za-z0-9.\\-_]{1,64}");

    private final String signatureAlias;
    private final String encryptionAlias;
    private final String signerPath;
    private final String signerPassword;
    private final FileResourceHelper helper;
    private KeyStore keystore;

    public SeedTrustManager(String seedPath, String seedPassword, String encryptionAlias, String signatureAlias, FileResourceHelper helper) {
        this.signerPath = seedPath;
        this.signerPassword = seedPassword;

        this.helper = helper;
        this.signatureAlias = signatureAlias;
        this.encryptionAlias = encryptionAlias;
    }

    public SeedSigner currentSigner() {
        try {
            var keyStore = openKeystore();
            ECKey key = ECKey.load(keyStore, this.signatureAlias, this.signerPassword.toCharArray());
            JWSSigner signer = new ECDSASigner(key);
            return new SeedSigner(this.signatureAlias, signer);
        } catch (IllegalArgumentException | KeyStoreException | JOSEException e) {
            throw cryptoException("signer", e);
        }
    }

    public SeedEncrypter currentEncrypter() {
        SecretKey key = encryptionKeyForKeyId(this.encryptionAlias);
        return new SeedEncrypter(this.encryptionAlias, key);
    }

    public SecretKey encryptionKeyForKeyId(String keyId) {
        keyId = safeKeyId(keyId);
        try {
            var keyStore = openKeystore();
            Key key = keyStore.getKey(keyId, this.signerPassword.toCharArray());
            if (key instanceof SecretKey secretKey) {
                return secretKey;
            } else {
                throw new SeedException(SeedException.Kind.CRYPTO, "encryption key unknown or in wrong format");
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw cryptoException("encryption", e);
        }
    }

    private SeedException cryptoException(String method, Exception e) {
        return new SeedException(SeedException.Kind.CRYPTO, "could not read the %s key".formatted(method), e);
    }

    private synchronized KeyStore openKeystore() {
        if (this.keystore == null) {
            this.keystore = this.helper.readKeyStore(this.signerPath, this.signerPassword);
        }
        return this.keystore;
    }

    public JWSVerifier verifierForKeyId(String keyId) {
        keyId = safeKeyId(keyId);
        var keyStore = openKeystore();
        try {
            ECKey key = ECKey.load(keyStore, keyId, this.signerPassword.toCharArray());
            if (key == null) {
                throw new SeedException(SeedException.Kind.INVALID, "no signing cert found for keyId");
            }
            return new ECDSAVerifier(key);
        } catch (KeyStoreException | JOSEException e) {
            throw cryptoException("signer", e);
        }
    }

    /**
     * Validates that the keyId (for encryption and signature) adheres to our
     * pattern and cannot be used to inject malicious data.
     */
    String safeKeyId(String keyId) {
        if (KEY_IDENTIFIER.matcher(keyId).matches()) {
            return keyId;
        }
        throw new SeedException(SeedException.Kind.INVALID, "key identifier has unexpected format");
    }

}
