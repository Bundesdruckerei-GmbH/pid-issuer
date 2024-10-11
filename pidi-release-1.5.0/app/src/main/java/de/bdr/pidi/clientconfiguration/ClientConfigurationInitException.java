/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.clientconfiguration;

public class ClientConfigurationInitException extends RuntimeException {
    public ClientConfigurationInitException(String message, Throwable cause) {
        super(message, cause);
    }
}
