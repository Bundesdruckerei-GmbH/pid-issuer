/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core.model;

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
