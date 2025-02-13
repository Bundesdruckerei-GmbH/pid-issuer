/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.configuration;

import de.governikus.panstar.sdk.saml.configuration.SamlConfiguration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * this SAML configuration supports
 * multiple signature certificates.
 *     (for use in a test client and on key rollover),
 *     this results in different <code>SamlKeyMaterial</code> instances<br>
 *     these are grouped in <code>SpecificConfiguration</code> instances</code>
 */
@Slf4j
@RequiredArgsConstructor
public class MultiSamlConfigurationImpl implements MultiSamlConfiguration {

    private final AutentConfigurationImpl config;

    private final String responseUrl;

    private List<SamlConfiguration> configurations;

    public void initConfigurations() {
        var temp = new ArrayList<SamlConfiguration>();
        var certs = this.config.getAutentSamlSignatureCertificates();
        for (var cert : certs) {
            temp.add(new SpecificConfiguration(this.config, this.responseUrl, cert));
        }
        this.configurations = temp;
    }

    @Override
    public List<SamlConfiguration> getConfigurations() {
        if (this.configurations == null) {
            initConfigurations();
        }
        return this.configurations;
    }
}
