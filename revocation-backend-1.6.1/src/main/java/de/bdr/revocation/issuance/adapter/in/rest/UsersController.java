/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.adapter.in.rest;

import de.bdr.revocation.issuance.adapter.in.rest.api.UsersApi;
import de.bdr.revocation.issuance.adapter.in.rest.api.model.IssuanceCount;
import de.bdr.revocation.issuance.app.service.IssuanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UsersController implements UsersApi {
    private final IssuanceService issuanceService;

    @Override
    public IssuanceCount serveIssuanceCount(String xSessionID) {
        log.info("Request issuance count for {}", xSessionID);
        var count = issuanceService.countIssuance(xSessionID);
        return new IssuanceCount(count.issued(), count.revocable());
    }

    @Override
    public void revokePIDs(String xSessionID) {
        log.info("Request revokation for {}", xSessionID);
        issuanceService.revokeIssuance(xSessionID);
    }
}
