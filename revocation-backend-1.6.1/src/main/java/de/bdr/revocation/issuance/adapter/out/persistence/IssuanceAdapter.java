/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.adapter.out.persistence;

import de.bdr.revocation.issuance.app.domain.Issuance;
import de.bdr.revocation.issuance.app.domain.IssuanceCount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class IssuanceAdapter {
    private final IssuanceRepository issuanceRepository;
    private final IssuanceMapper mapper;

    @Transactional
    public void save(final Issuance issuance) {
        IssuanceEntity entity = issuanceRepository.findByPseudonymAndListIdAndListIndex(issuance.getPseudonym(), issuance.getListID(), issuance.getIndex())
                .map(foundEntity -> {
                    log.warn("IssuanceEntity for {} will be updated", issuance.getPseudonym());
                    foundEntity.setExpirationTime(issuance.getExpirationTime());
                    return foundEntity;
                }).orElseGet(() -> {
                    log.debug("IssuanceEntity for {} will be inserted", issuance.getPseudonym());
                    return mapper.toEntity(issuance);
                });
        issuanceRepository.save(entity);
    }

    public IssuanceCount count(final String pseudonym) {
        return issuanceRepository.countIssuedAndRevocable(pseudonym);
    }

    public List<Issuance> getRevocable(final String pseudonym) {
        var revocable = issuanceRepository.getAllByPseudonymAndRevokedIsFalse(pseudonym);
        return revocable.stream().map(mapper::toDomain).toList();
    }

    @Transactional
    public void revoke(final Issuance issuance) {
        var entity = issuanceRepository
                .findByPseudonymAndListIdAndListIndex(issuance.getPseudonym(), issuance.getListID(), issuance.getIndex())
                .orElseThrow();
        entity.setRevoked(true);
        issuanceRepository.save(entity);
    }
}
