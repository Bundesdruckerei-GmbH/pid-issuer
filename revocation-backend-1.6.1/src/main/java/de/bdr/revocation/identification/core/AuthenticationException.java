/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core;

/**
 * A marker exception for validation failures in the Authentication process.
 * The <code>securityMessage</code> will not be shown to the user but logged.
 */
public interface AuthenticationException {

    String NO_VALID_AUTH = "No valid authentication";

    String getSecurityMessage();
}
