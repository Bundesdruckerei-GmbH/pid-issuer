/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core.configuration;

import de.bdr.pidi.identification.core.KeyAdapter;
import de.governikus.panstar.sdk.saml.configuration.SamlConfiguration;
import lombok.Getter;

import java.net.MalformedURLException;
import java.net.URI;
import java.security.cert.X509Certificate;

@Getter
public class SpecificConfiguration implements SamlConfiguration {

    private final KeyAdapter samlKeyMaterial;
    private final ServiceProviderConfiguration samlServiceProviderConfiguration;
    private final EidServerConfiguration samlEidServerConfiguration;

    public SpecificConfiguration(AutentConfigurationImpl autentConfiguration, String samlResponseUrl, X509Certificate signatureValidationCert) {
        try {
            this.samlServiceProviderConfiguration =
                    new ServiceProviderConfiguration(autentConfiguration.getServiceProviderName(),
                            URI.create(samlResponseUrl).toURL());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("could not construct response url from " + samlResponseUrl);
        }
        this.samlEidServerConfiguration = new EidServerConfiguration(autentConfiguration.getAutentSamlServiceUrl());
        this.samlKeyMaterial = loadKeyAdapter(autentConfiguration, signatureValidationCert);
    }

    private KeyAdapter loadKeyAdapter(AutentConfigurationImpl autentConfiguration, X509Certificate signatureValidationCert) {
        var signatureKeyParams = new KeyAdapter.KeyParams(
                autentConfiguration.getServiceProviderSignatureKeystore(),
                autentConfiguration.getSignatureAlias(),
                autentConfiguration.getSignatureKeyPassword());
        var decryptionKeyParams = new KeyAdapter.KeyParams(
                autentConfiguration.getServiceProviderDecryptionKeystore(),
                autentConfiguration.getDecryptionAlias(),
                autentConfiguration.getDecryptionKeyPassword());
        return new KeyAdapter(
                autentConfiguration.getAutentSamlEncryptionCertificate(),
                signatureValidationCert,
                signatureKeyParams,
                decryptionKeyParams);
    }
}
