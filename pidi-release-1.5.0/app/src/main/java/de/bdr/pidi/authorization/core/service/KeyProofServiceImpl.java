/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.service;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProof;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidProofException;
import de.bdr.pidi.base.IssuedAtValidationResult;
import de.bdr.pidi.base.IssuedAtValidator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class KeyProofServiceImpl extends ProofValidationService implements KeyProofService {
    public KeyProofServiceImpl(AuthorizationConfiguration configuration) {
        super(configuration);
    }

    @Override
    public JWK validateJwtProof(WSession session, JwtProof jwtProof, boolean issuerRequired) {
        var clientId = session.getCheckedParameter(SessionKey.CLIENT_ID);
        var nonce = session.getCheckedParameter(SessionKey.C_NONCE);
        var nonceExpirationTime = session.getCheckedParameterAsInstant(SessionKey.C_NONCE_EXP_TIME);

        var signedJwt = jwtProof.getSignedJwt();
        validateProofHeader(signedJwt.getHeader(), EXPECTED_PROOF_TYPE);
        var claims = parseJwtClaims(signedJwt);
        var requestTime = Instant.now();
        validateJwtProofClaims(claims, clientId, requestTime, session.getFlowVariant(), issuerRequired);
        validateNonce(claims, nonce, nonceExpirationTime, requestTime);

        verifySignature(signedJwt);
        return signedJwt.getHeader().getJWK();
    }

    private void validateJwtProofClaims(JWTClaimsSet claims, String clientId, Instant requestTime, FlowVariant flowVariant, boolean issuerRequired) {
        if (issuerRequired) {
            expect(clientId.equals(claims.getIssuer()), "Proof JWT issuer invalid");
        } else {
            expect(claims.getIssuer() == null || clientId.equals(claims.getIssuer()), "Proof JWT issuer invalid");
        }

        var audience = List.of(configuration.getCredentialIssuerIdentifier(flowVariant));
        expect(audience.equals(claims.getAudience()), "Proof JWT audience invalid");

        var proofTimeTolerance = configuration.getProofTimeTolerance();
        var proofValidity = configuration.getProofValidity();
        IssuedAtValidationResult issuedAtValidationResult = IssuedAtValidator.validate(claims.getIssueTime().toInstant(), requestTime, proofTimeTolerance, proofValidity);
        switch (issuedAtValidationResult) {
            case NOT_PRESENT -> throw exception("Proof JWT issuance is missing");
            case IN_FUTURE -> throw exception("Proof JWT is issued in the future");
            case TOO_OLD -> throw exception("Proof JWT issuance is too old");
            case VALID -> {
                // issued at ist valid, nothing to do
            }
        }
    }

    @Override
    void expect(boolean condition, String message) {
        if (!condition) {
            throw new InvalidProofException(message);
        }
    }

    @Override
    InvalidProofException exception(String message, Exception e) {
        return new InvalidProofException(message, e);
    }
    InvalidProofException exception(String message) {
        return new InvalidProofException(message);
    }
}
