/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
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
