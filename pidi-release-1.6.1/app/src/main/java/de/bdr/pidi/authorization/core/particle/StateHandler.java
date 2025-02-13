/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.ParameterTooLongException;

public class StateHandler implements OidHandler {

    public static final String STATE = "state";
    public static final int MAX_STATE_LENGTH = 2048;

    @Override
    public void processPushedAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        String state = request.getParameters().get(STATE);
        if (state != null) {
            if (state.length() > MAX_STATE_LENGTH) {
                throw new ParameterTooLongException(STATE, MAX_STATE_LENGTH);
            }
            session.putParameter(SessionKey.STATE, state);
        }
    }
}
