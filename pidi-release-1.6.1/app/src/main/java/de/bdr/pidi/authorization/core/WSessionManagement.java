/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core;

import de.bdr.pidi.authorization.core.domain.Requests;

public interface WSessionManagement {
    long getSessionId();
    void setNextExpectedRequest(Requests nextExpectedRequest);
}
