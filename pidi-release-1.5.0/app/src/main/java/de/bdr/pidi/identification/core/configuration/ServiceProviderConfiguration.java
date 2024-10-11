/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
