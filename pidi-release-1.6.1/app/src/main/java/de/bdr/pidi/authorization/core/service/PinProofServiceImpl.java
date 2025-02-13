/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.service;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.util.PinUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
public class PinProofServiceImpl extends ProofValidationService implements PinProofService {
    public PinProofServiceImpl(AuthorizationConfiguration configuration) {
        super(configuration);
    }

    @Override
    public JWK validatePinDerivedEphKeyPop(WSession session, SignedJWT pop) {
        var nonce = session.getCheckedParameter(SessionKey.C_NONCE);
        var nonceExpirationTime = session.getCheckedParameterAsInstant(SessionKey.C_NONCE_EXP_TIME);

        validateProofHeader(pop.getHeader(), EXPECTED_PIN_POP_TYPE);
        var claims = parseJwtClaims(pop);
        validatePinDerivedEphKeyPopClaims(claims, session.getFlowVariant(), NONCE_CLAIM);
        validateNonce(claims, nonce, nonceExpirationTime, Instant.now());

        verifySignature(pop);
        return pop.getHeader().getJWK();
    }

    @Override
    public JWK validatePinDerivedEphKeyPopTokenRequest(WSession session, SignedJWT pop) {
        var sessionId = session.getCheckedParameter(SessionKey.PID_ISSUER_SESSION_ID);
        var sessionIdExpirationTime = session.getCheckedParameterAsInstant(SessionKey.PID_ISSUER_SESSION_ID_EXP_TIME);

        validateProofHeader(pop.getHeader(), EXPECTED_PIN_POP_TYPE);
        var claims = parseJwtClaims(pop);
        validatePinDerivedEphKeyPopClaims(claims, session.getFlowVariant(), SESSION_ID_CLAIM);
        validateSessionId(claims, sessionId, sessionIdExpirationTime, Instant.now());

        verifySignature(pop);
        return pop.getHeader().getJWK();
    }

    @Override
    public JWK validateDeviceKeyPopTokenRequest(WSession session, SignedJWT pop) {
        var sessionId = session.getCheckedParameter(SessionKey.PID_ISSUER_SESSION_ID);
        var sessionIdExpirationTime = session.getCheckedParameterAsInstant(SessionKey.PID_ISSUER_SESSION_ID_EXP_TIME);

        validateProofHeader(pop.getHeader(), EXPECTED_DEVICE_KEY_TYPE);
        var claims = parseJwtClaims(pop);
        validateDeviceKeyPopClaims(claims, session.getFlowVariant());
        validateSessionId(claims, sessionId, sessionIdExpirationTime, Instant.now());

        verifySignature(pop);
        return pop.getHeader().getJWK();
    }

    private void validateDeviceKeyPopClaims(JWTClaimsSet claims, FlowVariant flowVariant) {
        var verifier = new DefaultJWTClaimsVerifier<>(
                new JWTClaimsSet.Builder().audience(configuration.getCredentialIssuerIdentifier(flowVariant)).build(),
                Set.of(JWTClaimNames.AUDIENCE, PinUtil.JWT_PIN_CLAIM, SESSION_ID_CLAIM));
        try {
            verifier.verify(claims, null);
        } catch (BadJWTException e) {
            throw exception("DeviceKeyPop claims invalid", e);
        }
    }

    private void validatePinDerivedEphKeyPopClaims(JWTClaimsSet claims, FlowVariant flowVariant, String nonceClaim) {
        var verifier = new DefaultJWTClaimsVerifier<>(
                new JWTClaimsSet.Builder().audience(configuration.getCredentialIssuerIdentifier(flowVariant)).build(),
                Set.of(JWTClaimNames.AUDIENCE, PinUtil.JWT_DEVICE_CLAIM, nonceClaim));
        try {
            verifier.verify(claims, null);
        } catch (BadJWTException e) {
            throw exception("PinDerivedEphKeyPop claims invalid", e);
        }
    }

    @Override
    void expect(boolean condition, String message) {
        if (!condition) {
            throw new InvalidRequestException(message);
        }
    }

    @Override
    InvalidRequestException exception(String message, Exception e) {
        return new InvalidRequestException(message, e);
    }
}
