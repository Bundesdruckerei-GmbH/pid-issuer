/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PinRetryCounterRepository extends JpaRepository<PinRetryCounterEntity, Long> {
    Optional<PinRetryCounterEntity> findFirstByDigest(String digest);
}
