/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.service;

import com.nimbusds.jose.jwk.JWK;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Collection;

@PrimaryPort
public interface IssuerMetadataService {
    Collection<JWK> getIssuerJwks();
}
