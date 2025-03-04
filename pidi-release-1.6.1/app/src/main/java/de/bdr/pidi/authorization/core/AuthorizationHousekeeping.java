/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core;

public interface AuthorizationHousekeeping {
    void cleanupExpiredSessions();
}
