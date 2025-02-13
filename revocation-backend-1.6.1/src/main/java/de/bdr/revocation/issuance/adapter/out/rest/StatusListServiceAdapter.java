/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.adapter.out.rest;

import de.bdr.revocation.issuance.app.domain.Issuance;
import de.bdr.revocation.issuance.adapter.out.rest.api.DefaultApi;
import de.bdr.revocation.issuance.adapter.out.rest.api.model.UpdateStatusRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class StatusListServiceAdapter {
    private static final Integer REVOKED_PID_STATUS_VALUE = 1;

    private final DefaultApi statusListServiceClient;

    public void updateStatus(@Valid @NotNull Issuance issuance) {
        val updateStatusRequest = new UpdateStatusRequest().uri(issuance.getListID()).index(issuance.getIndex()).value(REVOKED_PID_STATUS_VALUE);
        log.debug("update status with request: {}", updateStatusRequest);
        statusListServiceClient.updateStatus(updateStatusRequest);
        log.debug("update status successful for list {} and index {}", issuance.getListID(), issuance.getIndex());
    }
}
