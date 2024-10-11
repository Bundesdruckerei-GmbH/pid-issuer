/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
