/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.walletattestation.core;

import com.nimbusds.jwt.SignedJWT;

import java.time.Instant;

public interface ClientAttestationJwtBase {
    SignedJWT signedJWT();
    String iss();
    Instant exp();
    Instant nbf();
    Instant iat();
}
