/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core.configuration;


import de.bdr.pidi.identification.core.model.Authentication;

public interface Tr03124Configuration {

    String createAuthenticationLink(Authentication auth);

    String createSamlConsumerUrl();

    String createLoggedInUrl(String referenceId);

    String createFrontendLoggedInUrl(String referenceId, String lang);

    String createFrontendErrorUrl(String code);

    String getAuthParam();

}
