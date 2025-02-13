/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.configuration;

import de.bdr.revocation.identification.core.model.Authentication;

public interface Tr03124Configuration {

    String createAuthenticationLink(Authentication auth);

    String createLoggedInUrl(String referenceId);

    String getAuthParam();
}
