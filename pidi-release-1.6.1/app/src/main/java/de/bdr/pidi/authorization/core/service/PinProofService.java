/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.service;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import de.bdr.pidi.authorization.core.WSession;

public interface PinProofService {
    JWK validatePinDerivedEphKeyPop(WSession session, SignedJWT pop);
    JWK validatePinDerivedEphKeyPopTokenRequest(WSession session, SignedJWT pop);
    JWK validateDeviceKeyPopTokenRequest(WSession session, SignedJWT pop);
}
