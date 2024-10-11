/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.service;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProof;
import de.bdr.pidi.authorization.core.WSession;

import java.util.Collection;

public interface KeyProofService {
    JWK validateJwtProof(WSession session, final JwtProof jwt, boolean issuerRequired);
    default Collection<JWK> validateJwtProofs(WSession session, Collection<JwtProof> proofs, boolean issuerRequired) {
        return proofs.stream().map(proof -> validateJwtProof(session, proof, issuerRequired)).toList();
    }
}
