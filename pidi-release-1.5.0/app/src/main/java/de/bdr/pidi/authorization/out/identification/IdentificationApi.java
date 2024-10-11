/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
