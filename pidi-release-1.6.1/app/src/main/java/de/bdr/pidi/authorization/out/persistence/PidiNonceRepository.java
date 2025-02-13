/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PidiNonceRepository  extends JpaRepository<PidiNonceEntity, Long> {
    Optional<PidiNonceEntity> findFirstByNonce(String nonce);
}
