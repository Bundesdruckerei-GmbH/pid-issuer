/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.walletattestation;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import de.bdr.pidi.walletattestation.core.ClientAttestationJwtBase;

import java.text.ParseException;
import java.time.Instant;
import java.util.List;

public record ClientAttestationPopJwt(SignedJWT signedJWT, String iss, Instant exp, String jti, List<String> aud, Instant nbf, Instant iat) implements ClientAttestationJwtBase {

    public static ClientAttestationPopJwt from(SignedJWT signedJWT) throws ParseException {
        final var claims = signedJWT.getJWTClaimsSet();
        validateClaimsExist(claims);
        return new ClientAttestationPopJwt(
                signedJWT,
                claims.getIssuer(),
                claims.getExpirationTime().toInstant(),
                claims.getJWTID(),
                claims.getAudience(),
                claims.getNotBeforeTime() == null ? null : claims.getNotBeforeTime().toInstant(),
                claims.getIssueTime() == null ? null : claims.getIssueTime().toInstant()
        );
    }

    private static void validateClaimsExist(JWTClaimsSet claims) {
        if (claims.getIssuer() == null || claims.getIssuer().isBlank()) {
            throw new IllegalArgumentException("iss claim is missing");
        }
        if (claims.getExpirationTime() == null) {
            throw new IllegalArgumentException("exp claim is missing");
        }
        if (claims.getJWTID() == null || claims.getJWTID().isBlank()) {
            throw new IllegalArgumentException("jti claim is missing");
        }
        if (claims.getAudience() == null || claims.getAudience().isEmpty()) {
            throw new IllegalArgumentException("aud claim is missing");
        }
    }
}
