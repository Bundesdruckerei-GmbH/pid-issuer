/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.adapter.out.persistence;

import de.bdr.revocation.issuance.app.domain.Issuance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface IssuanceMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pseudonym")
    @Mapping(target = "listId", source = "listID")
    @Mapping(target = "listIndex", source = "index")
    @Mapping(target = "expirationTime")
    @Mapping(target = "revoked", constant = "false")
    IssuanceEntity toEntity(final Issuance issuance);

    @Mapping(target = "pseudonym")
    @Mapping(target = "listID", source = "listId")
    @Mapping(target = "index", source = "listIndex")
    @Mapping(target = "expirationTime")
    Issuance toDomain(final IssuanceEntity issuance);
}
