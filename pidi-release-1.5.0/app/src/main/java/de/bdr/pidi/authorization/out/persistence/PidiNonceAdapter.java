/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.out.persistence;

import de.bdr.pidi.authorization.core.domain.Nonce;
import de.bdr.pidi.authorization.core.domain.PidIssuerNonce;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PidiNonceAdapter {
    private final PidiNonceRepository pidiNonceRepository;

    @Transactional
    public PidIssuerNonce createAndSave(Nonce nonce, Duration pidiNonceLifetime) {
        PidiNonceEntity entity = new PidiNonceEntity();
        entity.setNonce(nonce.nonce());
        entity.setUsed(false);
        /*
        Postgress timestamp contains only microseconds and no nanoseconds
         */
        entity.setExpires(nonce.expirationTime().truncatedTo(ChronoUnit.MICROS));
        return map(pidiNonceRepository.save(entity), pidiNonceLifetime);
    }

    @Transactional
    public void setUsed(PidIssuerNonce pidiNonce) {
        PidiNonceEntity entity = pidiNonceRepository.getReferenceById(pidiNonce.getId());
        entity.setUsed(pidiNonce.isUsed());
        pidiNonceRepository.save(entity);
    }

    public Optional<PidIssuerNonce> findByNonce(String nonce, Duration pidiNonceLifetime) {
        return pidiNonceRepository.findFirstByNonce(nonce).map(pidiNonceEntity -> map(pidiNonceEntity, pidiNonceLifetime));
    }

    private PidIssuerNonce map(final PidiNonceEntity entity, Duration pidiNonceLifetime) {
        return new PidIssuerNonce(entity.getId(), new Nonce(entity.getNonce(), pidiNonceLifetime, entity.getExpires()), entity.isUsed());
    }
}
