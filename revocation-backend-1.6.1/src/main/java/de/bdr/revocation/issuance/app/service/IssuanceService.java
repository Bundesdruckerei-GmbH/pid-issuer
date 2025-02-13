/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.app.service;

import de.bdr.revocation.issuance.adapter.out.identification.IdentificationApi;
import de.bdr.revocation.issuance.adapter.out.persistence.IssuanceAdapter;
import de.bdr.revocation.issuance.adapter.out.rest.StatusListServiceAdapter;
import de.bdr.revocation.issuance.app.domain.Issuance;
import de.bdr.revocation.issuance.app.domain.IssuanceCount;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@RequiredArgsConstructor
@Service
public class IssuanceService {
    private static final String ERROR_SAVING_ISSUANCE_MSG = "Error saving issuance";
    private final IssuanceAdapter issuanceAdapter;
    private final StatusListServiceAdapter statusListServiceAdapter;
    private final IdentificationApi identificationApi;

    public void saveIssuance(final Issuance issuance) throws BusinessException {
        try {
            issuanceAdapter.save(issuance);
        } catch (DataIntegrityViolationException e) {
            log.error(ERROR_SAVING_ISSUANCE_MSG, e);
            throw new BusinessException(e);
        } catch (RuntimeException e) {
            log.error(ERROR_SAVING_ISSUANCE_MSG, e);
            throw new RevocationServerException(ERROR_SAVING_ISSUANCE_MSG, e);
        }
    }

    public IssuanceCount countIssuance(final String xSessionID) {
        String pseudonym = validateSessionAndGetPseudonym(xSessionID);
        return issuanceAdapter.count(pseudonym);
    }

    public void revokeIssuance(final String xSessionID) {
        String pseudonym = validateSessionAndGetPseudonym(xSessionID);
        var revocable = issuanceAdapter.getRevocable(pseudonym);
        AtomicInteger revoked = new AtomicInteger();
        AtomicBoolean gotException = new AtomicBoolean(false);
        revocable.forEach(i -> {
            try {
                statusListServiceAdapter.updateStatus(i);
                issuanceAdapter.revoke(i);
                revoked.getAndIncrement();
            } catch (RestClientResponseException e) {
                log.error("Error revoking issuance, continue with next issuance.", e);
                gotException.set(true);
            }
        });
        log.info("Revoked {}/{} issuance(s)", revoked.get(), revocable.size());
        if (gotException.get()) {
            throw new RevocationServerException("Some issuances could not be revoked.");
        }
    }

    private String validateSessionAndGetPseudonym(String xSessionID) {
        return identificationApi.validateSessionAndGetPseudonym(xSessionID).orElseThrow(IdentificationFailedException::new);
    }
}
