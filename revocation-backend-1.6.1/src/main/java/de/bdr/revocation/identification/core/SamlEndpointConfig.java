/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core;

public interface SamlEndpointConfig {

    /** the externally visible SAML consumer URL */
    String createSamlConsumerUrl();

}
