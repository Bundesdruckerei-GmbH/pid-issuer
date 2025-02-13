/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core.configuration;

import de.governikus.panstar.sdk.saml.configuration.SamlEidServerConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EidServerConfiguration implements SamlEidServerConfiguration {
    private final String samlRequestReceiverUrl;
}
