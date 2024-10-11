/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.config;

import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialRequest;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest;
import de.bdr.openid4vc.common.signing.Pkcs12Signer;
import de.bdr.openid4vc.common.vci.proofs.cwt.CwtProofType;
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProofType;
import de.bdr.openid4vc.vci.credentials.FeatureMode;
import de.bdr.openid4vc.vci.credentials.mdoc.CredentialStructure;
import de.bdr.openid4vc.vci.credentials.mdoc.MDocCredentialConfiguration;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.out.issuance.MdocBuilder;
import de.bdr.pidi.authorization.out.issuance.SdJwtBuilder;
import de.bdr.pidi.base.FileResourceHelper;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialRequest;
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialRequest;
import de.bdr.pidi.issuance.core.IssuanceConfiguration;
import de.bdr.pidi.issuance.core.service.DeviceSignedMdocCreator;
import de.bdr.pidi.issuance.core.service.IssuerSignedMdocCreator;
import de.bdr.pidi.issuance.core.service.PidSdJwtVcCreator;
import de.bdr.pidi.issuance.core.service.SeedPidService;
import de.bdr.pidi.issuance.core.service.SeedTrustManager;
import de.bdr.pidi.issuance.in.DeviceSignedMdocBuilder;
import de.bdr.pidi.issuance.in.DvsSignedSdJwtBuilder;
import de.bdr.pidi.issuance.in.IssuerSignedMdocBuilder;
import de.bdr.pidi.issuance.in.IssuerSignedSdJwtBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static de.bdr.pidi.base.FileResourceHelper.getFileInputStream;
import static de.bdr.pidi.base.PidDataConst.MDOC_ID;
import static de.bdr.pidi.base.PidDataConst.MDOC_TYPE;

@Configuration
public class PidDataBuilderConfig {

    private final String baseUrl;
    private final String signerPath;
    private final String signerPassword;
    private final String signerAlias;
    private final String seedPath;
    private final String seedPassword;
    private final String seedEncAlias;
    private final String seedSigAlias;
    private final Duration lifetime;
    private final Duration seedValidity;

    public PidDataBuilderConfig(IssuanceConfiguration configuration) {
        var tmp = configuration.getBaseUrl().toString();
        if (tmp.endsWith("/")) {
            baseUrl = tmp;
        } else {
            baseUrl = tmp + "/";
        }
        signerPath = configuration.getSignerPath();
        signerPassword = configuration.getSignerPassword();
        signerAlias = configuration.getSignerAlias();
        lifetime = configuration.getLifetime();
        seedValidity = configuration.getSeedValidity();
        seedPath = configuration.getSeedPath();
        seedPassword = configuration.getSeedPassword();
        seedEncAlias = configuration.getSeedEncAlias();
        seedSigAlias = configuration.getSeedSigAlias();
    }

    @Bean
    public SdJwtBuilder<SdJwtVcCredentialRequest> c2SdJwtBuilder() {
        return new IssuerSignedSdJwtBuilder(getIssuerSigSdJwtVcCreator(FlowVariant.C2));
    }

    @Bean
    public SdJwtBuilder<SdJwtVcCredentialRequest> c1SdJwtBuilder() {
        return new IssuerSignedSdJwtBuilder(getIssuerSigSdJwtVcCreator(FlowVariant.C1));
    }

    @Bean
    public SdJwtBuilder<SdJwtVcCredentialRequest> cSdJwtBuilder() {
        return new IssuerSignedSdJwtBuilder(getIssuerSigSdJwtVcCreator(FlowVariant.C));
    }

    @Bean
    public SdJwtBuilder<SdJwtVcAuthChannelCredentialRequest> bSdJwtBuilder() {
        return new DvsSignedSdJwtBuilder(baseUrl + FlowVariant.B.urlPath, lifetime, getSignerKey(), getSignerCertificates());
    }

    @Bean
    public SdJwtBuilder<SdJwtVcAuthChannelCredentialRequest> b1SdJwtBuilder() {
        return new DvsSignedSdJwtBuilder(baseUrl + FlowVariant.B1.urlPath, lifetime, getSignerKey(), getSignerCertificates());
    }

    @Bean
    public MdocBuilder<MsoMdocCredentialRequest> cMdocBuilder() {
        return new IssuerSignedMdocBuilder(new IssuerSignedMdocCreator(getMDocCredentialConfig(CredentialStructure.ISSUER_SIGNED), getSigner()));
    }

    @Bean
    public MdocBuilder<MsoMdocAuthChannelCredentialRequest> bMdocBuilder() {
        return new DeviceSignedMdocBuilder(new DeviceSignedMdocCreator(getMDocCredentialConfig(CredentialStructure.DOCUMENT), getSignerCertificates()), getSignerKey());
    }

    private PidSdJwtVcCreator getIssuerSigSdJwtVcCreator(FlowVariant flowVariant) {
        return new PidSdJwtVcCreator(baseUrl + flowVariant.urlPath, getSigner(), lifetime);
    }

    public List<X509Certificate> getSignerCertificates() {
        try {
            KeyStore ks = KeyStore.getInstance("pkcs12");
            ks.load(getFileInputStream(signerPath), signerPassword.toCharArray());
            return Arrays.stream(ks.getCertificateChain(signerAlias)).map(X509Certificate.class::cast).toList();
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
            throw new IllegalArgumentException("Could not get cert chain with alias \"%s\" from keystore".formatted(signerAlias), e);
        }
    }

    @NotNull
    public ECPrivateKey getSignerKey() {
        try {
            var ks = KeyStore.getInstance("pkcs12");
            ks.load(getFileInputStream(signerPath), signerPassword.toCharArray());
            Key key = ks.getKey(signerAlias, signerPassword.toCharArray());
            if (key == null) {
                throw new IllegalArgumentException("Keystore has no key with alias \"%s\"".formatted(signerAlias));
            }
            if (key instanceof ECPrivateKey ecPrivateKey) {
                return ecPrivateKey;
            } else {
                throw new IllegalArgumentException("Key with alias \"%s\" is not an elliptic curve private key".formatted(signerAlias));
            }
        } catch (UnrecoverableKeyException | CertificateException | NoSuchAlgorithmException | KeyStoreException |
                 IOException e) {
            throw new IllegalArgumentException("Could not get signer key with alias \"%s\" from keystore".formatted(signerAlias), e);
        }
    }

    @NotNull
    public Pkcs12Signer getSigner() {
        try {
            return new Pkcs12Signer(Objects.requireNonNull(getFileInputStream(signerPath)), signerPassword);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Could not work with the keystore from " + signerPath, e);
        }
    }

    private MDocCredentialConfiguration getMDocCredentialConfig(CredentialStructure structure) {
        return new MDocCredentialConfiguration(MDOC_ID, FeatureMode.DISABLED, FeatureMode.DISABLED, FeatureMode.DISABLED, FeatureMode.OPTIONAL,
                null, Set.of(JwtProofType.INSTANCE, CwtProofType.INSTANCE), null, lifetime, MDOC_TYPE, structure);
    }

    @Bean
    public SeedPidService seedPidService(FileResourceHelper helper) {
        var trustManager = new SeedTrustManager(seedPath, seedPassword, seedEncAlias, seedSigAlias, helper);
        return new SeedPidService(trustManager, seedValidity);
    }
}
