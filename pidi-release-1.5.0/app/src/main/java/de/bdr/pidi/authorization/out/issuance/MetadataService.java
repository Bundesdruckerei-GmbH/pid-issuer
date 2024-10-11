/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.out.issuance;

import com.nimbusds.jose.jwk.JWK;

import java.util.Collection;

public interface MetadataService {
    Collection<JWK> getJwks();
}
