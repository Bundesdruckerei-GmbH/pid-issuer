/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.walletattestation;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import de.bdr.openid4vc.vci.service.attestation.ClientAttestationCnf;
import de.bdr.pidi.walletattestation.core.ClientAttestationJwtBase;

import java.text.ParseException;
import java.time.Instant;
import java.util.Map;

public record ClientAttestationJwt(SignedJWT signedJWT, String iss, String sub, Instant exp, Instant nbf, Instant iat, ClientAttestationCnf cnf) implements ClientAttestationJwtBase {

    public static ClientAttestationJwt from(SignedJWT signedJWT) throws ParseException {
        final var claims = signedJWT.getJWTClaimsSet();
        validateClaimsExist(claims);
        final var cnf = new ClientAttestationCnf(getCnfClaim(claims));
        return new ClientAttestationJwt(
                signedJWT,
                claims.getIssuer(),
                claims.getSubject(),
                claims.getExpirationTime().toInstant(),
                claims.getNotBeforeTime() == null ? null : claims.getNotBeforeTime().toInstant(),
                claims.getIssueTime() == null ? null : claims.getIssueTime().toInstant(),
                cnf
        );
    }

    private static void validateClaimsExist(JWTClaimsSet claims) throws ParseException {
        if (claims.getIssuer() == null || claims.getIssuer().isBlank()) {
            throw new IllegalArgumentException("iss claim is missing");
        }
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new IllegalArgumentException("sub claim is missing");
        }
        if (claims.getExpirationTime() == null) {
            throw new IllegalArgumentException("exp claim is missing");
        }
        final var cnfClaim = getCnfClaim(claims);
        if (cnfClaim == null || cnfClaim.isEmpty()) {
            throw new IllegalArgumentException("cnf claim is missing");
        }
    }

    private static Map<String, Object> getCnfClaim(JWTClaimsSet claims) throws ParseException {
        return claims.getJSONObjectClaim("cnf");
    }
}
