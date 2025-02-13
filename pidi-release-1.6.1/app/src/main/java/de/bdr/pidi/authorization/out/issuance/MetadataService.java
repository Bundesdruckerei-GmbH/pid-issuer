/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.out.issuance;

import com.nimbusds.jose.jwk.JWK;

import java.util.Collection;

public interface MetadataService {
    Collection<JWK> getJwks();
}
