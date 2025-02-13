/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.exception.OIDException;

import java.text.ParseException;
import java.time.Instant;

import static de.bdr.openid4vc.vci.data.Constants.PROOF_TYPE_OPENID4VCI_PROOF_JWT;

public abstract class ProofValidationService {
    final AuthorizationConfiguration configuration;
    static final JOSEObjectType EXPECTED_PROOF_TYPE = new JOSEObjectType(PROOF_TYPE_OPENID4VCI_PROOF_JWT);
    static final JOSEObjectType EXPECTED_PIN_POP_TYPE = new JOSEObjectType("pin_derived_eph_key_pop");
    static final JOSEObjectType EXPECTED_DEVICE_KEY_TYPE = new JOSEObjectType("device_key_pop");
    static final String SESSION_ID_CLAIM = "pid_issuer_session_id";
    static final String NONCE_CLAIM = "nonce";

    ProofValidationService(AuthorizationConfiguration configuration) {
        this.configuration = configuration;
    }

    void validateProofHeader(JWSHeader header, JOSEObjectType proofType) {
        // necessary? is also checked by ECDSAVerifier.verify
        expect(JWSAlgorithm.ES256.equals(header.getAlgorithm()), "Proof JWT algorithm mismatch, expected to be " + JWSAlgorithm.ES256.getName());

        expect(proofType.equals(header.getType()), "Proof JWT type mismatch, expected to be " + proofType.getType());

        expect(header.getJWK() != null, "Proof JWT header should contain a JWK");

        expect(header.getKeyID() == null, "Proof JWT header keyId should not be present");

        expect(header.getX509CertChain() == null || header.getX509CertChain().isEmpty(), "Proof JWT header X509CertChain should not be present");

        expect(header.getCustomParam("trust_chain") == null, "Proof JWT header trust chain should not be present");
    }

    void validateSessionId(JWTClaimsSet claims, String sessionId, Instant sessionIdExpirationTime, Instant requestTime) {
        validateNonce(claims, sessionId, sessionIdExpirationTime, requestTime, SESSION_ID_CLAIM);
    }

    void validateNonce(JWTClaimsSet claims, String nonce, Instant nonceExpirationTime, Instant requestTime) {
        validateNonce(claims, nonce, nonceExpirationTime, requestTime, NONCE_CLAIM);
    }

    void validateNonce(JWTClaimsSet claims, String nonce, Instant nonceExpirationTime, Instant requestTime, String claim) {
        var proofTimeTolerance = configuration.getProofTimeTolerance();
        expect(requestTime.minus(proofTimeTolerance).isBefore(nonceExpirationTime), "Proof JWT credential " + claim + " expired");
        expect(nonce.equals(claims.getClaim(claim)), "Proof JWT credential " + claim + " invalid");
    }

    void verifySignature(SignedJWT signedJwt) {
        try {
            var verifier = new ECDSAVerifier(signedJwt.getHeader().getJWK().toECKey());
            expect(signedJwt.verify(verifier), "Proof JWT signature is invalid");
        } catch (JOSEException e) {
            throw exception("Proof signature could not be verified", e);
        }
    }

    JWTClaimsSet parseJwtClaims(JWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw exception("Proof JWT claims could not be parsed", e);
        }
    }

    abstract void expect(boolean condition, String message);
    abstract OIDException exception(String message, Exception e);
}
