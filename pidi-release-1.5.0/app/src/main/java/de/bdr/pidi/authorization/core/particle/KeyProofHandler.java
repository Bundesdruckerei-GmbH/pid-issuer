/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.common.vci.proofs.Proof;
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProof;
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProofType;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidCredentialRequestException;
import de.bdr.pidi.authorization.core.exception.InvalidProofException;
import de.bdr.pidi.authorization.core.service.KeyProofService;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;

/**
 * needs to be executed before the CNonceIssuanceHandler
 */
@RequiredArgsConstructor
public class KeyProofHandler implements OidHandler {

    private final KeyProofService keyProofService;
    private final List<Class<? extends CredentialRequest>> requestsUsingProof;
    private final boolean useProofs;

    @Override
    public void processCredentialRequest(HttpRequest<CredentialRequest> request, WResponseBuilder response, WSession session) {
        var credentialRequest = request.getBody();
        var proof = credentialRequest.getProof();
        var proofs = credentialRequest.getProofs();
        if (!requestsUsingProof.contains(credentialRequest.getClass())) {
            if (proof != null || !proofs.isEmpty()) {
                throw new InvalidCredentialRequestException("Neither proof nor proofs expected");
            }
            return;
        }
        checkProofsIfExpected(proofs, proof);

        if (proof == null) {
            var jwtProofs = checkJwtProofType(proofs);
            var jwks = keyProofService.validateJwtProofs(session, jwtProofs, true);
            var jwkJsons = jwks.stream().map(JWK::toJSONString).toList();
            session.putParameters(SessionKey.VERIFIED_CREDENTIAL_KEYS, jwkJsons);
        } else {
            var jwtProof = checkJwtProofType(proof);
            var jwk = keyProofService.validateJwtProof(session, jwtProof, true);
            session.putParameter(SessionKey.VERIFIED_CREDENTIAL_KEY, jwk.toJSONString());
        }
    }

    private List<JwtProof> checkJwtProofType(Collection<Proof> proofs) {
        return proofs.stream().map(this::checkJwtProofType).toList();
    }

    private JwtProof checkJwtProofType(Proof proof) {
        if (!JwtProofType.INSTANCE.equals(proof.getProofType())) {
            throw new InvalidProofException("Proof type invalid");
        }
        return (JwtProof) proof;
    }

    private void checkProofsIfExpected(List<Proof> proofs, Proof proof) {
        if (!useProofs && !proofs.isEmpty()) {
            throw new InvalidCredentialRequestException("No proofs expected");
        }
        if (proof == null && proofs.isEmpty()) {
            throw new InvalidProofException("Proof is missing");
        }
        if (proof != null && !proofs.isEmpty()) {
            throw new InvalidProofException("Proof and proofs MUST NOT be set at the same time.");
        }
    }
}
