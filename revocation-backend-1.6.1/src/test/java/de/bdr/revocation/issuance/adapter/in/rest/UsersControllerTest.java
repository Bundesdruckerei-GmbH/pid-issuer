/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.adapter.in.rest;

import de.bdr.revocation.issuance.TestUtils;
import de.bdr.revocation.issuance.app.service.IssuanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UsersControllerTest {

    @Mock
    private IssuanceService issuanceService;
    @InjectMocks
    private UsersController usersController;

    @DisplayName("should fetch issuance count")
    @Test
    void test001() {
        var serviceCount = TestUtils.createIssuanceCount();
        doReturn(serviceCount).when(issuanceService).countIssuance("pseudo");
        var controllerCount = usersController.serveIssuanceCount("pseudo");
        assertThat(controllerCount.getIssued()).isEqualTo(serviceCount.issued());
        assertThat(controllerCount.getRevocable()).isEqualTo(serviceCount.revocable());
    }

    @DisplayName("should revoke issuance")
    @Test
    void test002() {
        usersController.revokePIDs("pseudo");
        verify(issuanceService).revokeIssuance("pseudo");
    }
}