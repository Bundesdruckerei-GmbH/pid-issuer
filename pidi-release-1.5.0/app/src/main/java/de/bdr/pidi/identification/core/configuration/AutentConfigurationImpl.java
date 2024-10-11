/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core.configuration;

import de.bdr.pidi.base.FileResourceHelper;
import de.bdr.pidi.identification.config.IdentificationConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Configuration required to access Panstar via SAML.
 * <p>
 * We assume that the keystore and the keys have the same password.
 */
@Component
@RequiredArgsConstructor
public class AutentConfigurationImpl {

    private final IdentificationConfiguration identificationConfiguration;
    private final FileResourceHelper fileResourceHelper;

    /**
     * from Autent SamlConfiguration
     */
    public String getAutentSamlServiceUrl() {
        return identificationConfiguration.getServer().getUrl();
    }

    /**
     * from Autent SamlConfiguration
     */
    public X509Certificate getAutentSamlSignatureCertificate() {
        return this.fileResourceHelper.readCertificate(identificationConfiguration.getServer().getCertificateSigPaths().getFirst());
    }

    /**
     * from Autent SamlConfiguration
     */
    public X509Certificate getAutentSamlEncryptionCertificate() {
        return this.fileResourceHelper.readCertificate(identificationConfiguration.getServer().getCertificateEncPath());
    }

    public List<X509Certificate> getAutentSamlSignatureCertificates() {
        return identificationConfiguration.getServer().getCertificateSigPaths().stream().map(fileResourceHelper::readCertificate).toList();
    }

    /**
     * from Autent SamlConfiguration
     */
    public String getServiceProviderName() {
        return identificationConfiguration.getServiceProviderName();
    }

    /**
     * from Autent SamlConfiguration
     */
    public KeyStore getServiceProviderSignatureKeystore() {
        return this.fileResourceHelper.readKeyStore(identificationConfiguration.getXmlsigKeystore().getPath(),
                identificationConfiguration.getXmlsigKeystore().getPassword());
    }

    /**
     * from Autent SamlConfiguration
     */
    public KeyStore getServiceProviderDecryptionKeystore() {
        return this.fileResourceHelper.readKeyStore(identificationConfiguration.getXmlencKeystore().getPath(),
                identificationConfiguration.getXmlencKeystore().getPassword());
    }

    /**
     * from Autent SamlConfiguration
     */
    public String getSignatureAlias() {
        return identificationConfiguration.getXmlsigKeystore().getAlias();
    }

    /**
     * from Autent SamlConfiguration
     */
    public String getDecryptionAlias() {
        return identificationConfiguration.getXmlencKeystore().getAlias();
    }

    /**
     * from Autent SamlConfiguration
     */
    public String getSignatureKeyPassword() {
        return identificationConfiguration.getXmlsigKeystore().getKeyPassword();
    }

    /**
     * from Autent SamlConfiguration
     */
    public String getDecryptionKeyPassword() {
        return identificationConfiguration.getXmlencKeystore().getKeyPassword();
    }
}
