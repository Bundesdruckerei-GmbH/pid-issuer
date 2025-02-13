/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.model;

public enum AuthenticationState {
    INITIALIZED,
    /** the SAML AuthnRequest was generated */
    STARTED,
    /** the SAML Response was received */
    RESPONDED,
    /** the user has authenticated successfully, is logged in */
    AUTHENTICATED,
    /** the session has been timed out by the housekeeping service */
    TIMEOUT,
    /** the user terminated the session via logout */
    TERMINATED
}
