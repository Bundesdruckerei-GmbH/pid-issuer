/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.service;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.pidi.authorization.out.issuance.MetadataService;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class IssuerMetadataServiceImpl implements IssuerMetadataService {
    private final MetadataService metadataService;

    public IssuerMetadataServiceImpl(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @Override
    public Collection<JWK> getIssuerJwks() {
        return metadataService.getJwks();
    }
}
