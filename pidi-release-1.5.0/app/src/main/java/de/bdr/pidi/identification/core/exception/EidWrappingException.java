/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core.exception;

/**
 * This interface marks errors that happen during eID authentication
 * at the TR-03124 interface.
 * It may wrap other exceptions as <code>cause</code>.
 */
public interface EidWrappingException {

    String ERR_CODE_ABORTED = "ABORTED";
    String ERR_CODE_SYSTEM = "SERVICE_UNAVAILABLE";

    String getVisibleCode();

    /**
     * signals whether the error occurred in the initial call
     * that generates the SAMLAuthnRequest (<code>true</code>)
     * or in the call that processes the response.
     */
    boolean inSamlRequest();

}
