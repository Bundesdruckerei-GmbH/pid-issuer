/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.adapter.out.persistence;

import de.bdr.revocation.issuance.app.domain.IssuanceCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IssuanceRepository extends JpaRepository<IssuanceEntity, Long> {
    Optional<IssuanceEntity> findByPseudonymAndListIdAndListIndex(String pseudonym, String listId, Integer listIndex);

    @Query("""
            select new de.bdr.revocation.issuance.app.domain.IssuanceCount(
                cast(count(*) as int),
                cast(coalesce(sum(case when revoked = false then 1 else 0 end), 0) as int)
            )
            from IssuanceEntity
            where pseudonym = ?1
            """)
    IssuanceCount countIssuedAndRevocable(String pseudonym);

    List<IssuanceEntity> getAllByPseudonymAndRevokedIsFalse(String pseudonym);
}
