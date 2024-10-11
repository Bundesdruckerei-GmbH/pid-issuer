/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core;

import de.bdr.pidi.authorization.core.domain.Requests;

public interface WSessionManagement {
    long getSessionId();
    void setNextExpectedRequest(Requests nextExpectedRequest);
}
