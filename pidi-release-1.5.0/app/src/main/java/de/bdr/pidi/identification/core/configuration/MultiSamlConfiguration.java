/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core.configuration;

import de.governikus.panstar.sdk.saml.configuration.SamlConfiguration;

import java.util.List;

/**
 * We support multiple response signature validation certificates.
 * Each one is represented by a separate <code>SamlConfiguration</code>.
 */
public interface MultiSamlConfiguration {

    List<SamlConfiguration> getConfigurations();

    String getResponseUrl();

}
