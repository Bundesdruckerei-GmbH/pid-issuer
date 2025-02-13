/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.out.identification;

import java.net.URL;

/**
 * The project public api identification
 */
public interface IdentificationApi {

    URL startIdentificationProcess(URL redirectUrl, String issuerState, String sessionId);

    interface IdentificationResultCallback {
        void identificationError(String issuerState, String errorDescription);
        void successfulIdentification(String issuerState, PidCredentialData pidData);
    }
}
