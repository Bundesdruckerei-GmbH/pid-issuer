/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.in;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.KeyLengthException;
import de.bdr.openid4vc.common.Algorithm;
import de.bdr.openid4vc.common.signing.DVSP256SHA256HS256DelegateSigner;
import de.bdr.openid4vc.common.signing.KeyMaterial;
import de.bdr.openid4vc.common.signing.Signer;
import de.bdr.openid4vc.common.signing.X509KeyMaterial;
import de.bdr.openid4vc.common.signing.nimbus.DVSP256SHA256HS256MacSigner;
import de.bdr.openid4vc.common.signing.nimbus.DVSP256SHA256Key;
import de.bdr.pidi.authorization.out.issuance.FaultyRequestParameterException;
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialRequest;
import de.bdr.pidi.issuance.core.service.PidSdJwtVcCreator;
import org.jetbrains.annotations.NotNull;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.util.List;

public class DvsSignedSdJwtBuilder extends SdJwtBuilderImpl<SdJwtVcAuthChannelCredentialRequest> {
    private final String publicUrl;
    private final Duration lifetime;
    private final ECPrivateKey signerPrivateKey;
    private final X509KeyMaterial signerX509KeyMaterial;

    public DvsSignedSdJwtBuilder(String publicUrl, Duration lifetime, ECPrivateKey signerPrivateKey, List<X509Certificate> certificateChain) {
        this.publicUrl = publicUrl;
        this.lifetime = lifetime;
        this.signerPrivateKey = signerPrivateKey;
        this.signerX509KeyMaterial = new X509KeyMaterial(certificateChain);
    }

    @Override
    PidSdJwtVcCreator provideCreator(SdJwtVcAuthChannelCredentialRequest credentialRequest) {
        ECPublicKey verifierPub = getVerifierPub(credentialRequest);
        return buildDvsSdJwtVcCreator(verifierPub);
    }

    @Override
    String getVct(SdJwtVcAuthChannelCredentialRequest credentialRequest) {
        return credentialRequest.getVct();
    }

    private ECPublicKey getVerifierPub(SdJwtVcAuthChannelCredentialRequest credentialRequest) {
        try {
            return credentialRequest.getVerifierPub().toECKey().toECPublicKey();
        } catch (JOSEException | ClassCastException e) {
            throw new FaultyRequestParameterException("verifierPub is no valid ec key", e);
        }
    }

    // TODO this delegate only serves to fix the getKeys method of the (final) class DVSP256SHA256HS256DelegateSigner - should be fixed in base lib
    static class CertificateChainProvidingDelegateSigner implements Signer {
        private final Signer signer;
        private final X509KeyMaterial keyMaterial;

        public CertificateChainProvidingDelegateSigner(DVSP256SHA256HS256DelegateSigner signer, X509KeyMaterial keyMaterial) {
            this.signer = signer;
            this.keyMaterial = keyMaterial;
        }

        @NotNull
        @Override
        public Algorithm getAlgorithm() {
            return signer.getAlgorithm();
        }

        @NotNull
        @Override
        public KeyMaterial getKeys() {
            return keyMaterial;
        }

        @NotNull
        @Override
        public byte[] sign(@NotNull byte[] bytes) {
            return signer.sign(bytes);
        }
    }

    // TODO With the current version of the openid4vc lib, a new signer and creator need to be created. That will change with a future version of the lib
    private PidSdJwtVcCreator buildDvsSdJwtVcCreator(ECPublicKey verifierPub) {
        DVSP256SHA256HS256MacSigner dvsMacSigner;
        try {
            dvsMacSigner = new DVSP256SHA256HS256MacSigner(new DVSP256SHA256Key(signerPrivateKey, verifierPub));
        } catch (NoSuchAlgorithmException | InvalidKeyException | KeyLengthException e) {
            throw new IllegalArgumentException("Invalid verifier key", e);
        }

        var dvsDelegateSigner = new DVSP256SHA256HS256DelegateSigner(dvsMacSigner);
        var certificateChainProvidingDelegateSigner = new CertificateChainProvidingDelegateSigner(dvsDelegateSigner, signerX509KeyMaterial);
        return new PidSdJwtVcCreator(publicUrl, certificateChainProvidingDelegateSigner, lifetime);
    }
}
