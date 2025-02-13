/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core.configuration;

import de.governikus.panstar.sdk.saml.configuration.SamlServiceProviderConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URL;
import java.util.Optional;

@RequiredArgsConstructor
public class ServiceProviderConfiguration implements SamlServiceProviderConfiguration {

    @Getter
    private final String samlEntityId;
    private final URL samlResponseReceiverUrl;

    @Override
    public Optional<URL> getSamlResponseReceiverUrl() {
        return Optional.of(this.samlResponseReceiverUrl);
    }
}
