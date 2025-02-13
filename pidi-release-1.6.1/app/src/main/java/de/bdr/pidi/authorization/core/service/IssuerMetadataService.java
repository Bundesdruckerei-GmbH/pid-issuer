/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.service;

import com.nimbusds.jose.jwk.JWK;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Collection;

@PrimaryPort
public interface IssuerMetadataService {
    Collection<JWK> getIssuerJwks();
}
