/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.clientconfiguration;

public class ClientConfigurationInitException extends RuntimeException {
    public ClientConfigurationInitException(String message, Throwable cause) {
        super(message, cause);
    }
}
