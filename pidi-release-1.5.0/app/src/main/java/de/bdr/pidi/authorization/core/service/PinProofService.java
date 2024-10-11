/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
