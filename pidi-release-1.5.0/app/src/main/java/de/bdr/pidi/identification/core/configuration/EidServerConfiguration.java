/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
