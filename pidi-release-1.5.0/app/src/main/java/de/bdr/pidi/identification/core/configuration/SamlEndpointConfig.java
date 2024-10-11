/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core.configuration;

public interface SamlEndpointConfig {

    /** the externally visible SAML consumer URL */
    String createSamlConsumerUrl();

}
