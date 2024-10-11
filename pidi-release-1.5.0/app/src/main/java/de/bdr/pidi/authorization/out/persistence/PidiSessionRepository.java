/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PidiSessionRepository extends JpaRepository<PidiSessionEntity, Long> {
    Optional<PidiSessionEntity> findFirstByRequestUri(String requestUri);
    Optional<PidiSessionEntity> findFirstByAuthorizationCode(String authorizationCode);
    Optional<PidiSessionEntity> findFirstByIssuerState(String issuerState);
    Optional<PidiSessionEntity> findFirstByAccessToken(String accessToken);
    Optional<PidiSessionEntity> findFirstByRefreshTokenDigest(String refreshTokenDigest);
    Optional<PidiSessionEntity> findFirstByPidIssuerSessionId(String pipIssuerSessionId);
    int deleteAllByExpiresBefore(Instant now);
}
