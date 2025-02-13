/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.app.service;

import de.bdr.revocation.issuance.TestUtils;
import de.bdr.revocation.issuance.adapter.out.identification.IdentificationApi;
import de.bdr.revocation.issuance.adapter.out.persistence.IssuanceAdapter;
import de.bdr.revocation.issuance.adapter.out.rest.StatusListServiceAdapter;
import de.bdr.revocation.issuance.app.domain.Issuance;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class IssuanceServiceTest {

    private final Issuance issuance = TestUtils.createIssuance();

    @Mock
    private IssuanceAdapter issuanceAdapter;

    @Mock
    private StatusListServiceAdapter statusListServiceAdapter;

    @Mock
    private IdentificationApi identificationApi;

    @InjectMocks
    private IssuanceService issuanceService;

    @Nested
    class SaveIssuance {

        @Test
        void shouldSaveIssuance() throws BusinessException {
            issuanceService.saveIssuance(issuance);

            Mockito.verify(issuanceAdapter).save(issuance);
        }

        @Test
        void shouldHandleServerException() {
            Mockito.doThrow(RuntimeException.class).when(issuanceAdapter).save(issuance);

            assertThrows(RevocationServerException.class, () -> issuanceService.saveIssuance(issuance));
            Mockito.verify(issuanceAdapter).save(issuance);
        }

        @Test
        void shouldHandleBusinessException() {
            Mockito.doThrow(DataIntegrityViolationException.class).when(issuanceAdapter).save(issuance);

            assertThrows(BusinessException.class, () -> issuanceService.saveIssuance(issuance));
            Mockito.verify(issuanceAdapter).save(issuance);
        }
    }

    @Nested
    class CountIssuance {

        @Test
        void shouldPassIssuanceCount() {
            var adapterCount = TestUtils.createIssuanceCount();
            doReturn(Optional.of("pseudo")).when(identificationApi).validateSessionAndGetPseudonym(anyString());
            doReturn(adapterCount).when(issuanceAdapter).count("pseudo");
            var serviceCount = issuanceService.countIssuance("sessionId");
            assertThat(serviceCount).isEqualTo(adapterCount);
        }

        @Test
        void shouldErrorWhenIdentificationFailed() {
            doReturn(Optional.empty()).when(identificationApi).validateSessionAndGetPseudonym(anyString());
            assertThrows(IdentificationFailedException.class, () -> issuanceService.countIssuance("sessionId"));
        }
    }

    @Nested
    class RevokeIssuance {

        @Test
        void shouldRevokeIssuance() {
            doReturn(Optional.of("pseudo")).when(identificationApi).validateSessionAndGetPseudonym(anyString());
            doReturn(List.of(issuance)).when(issuanceAdapter).getRevocable("pseudo");
            Mockito.doNothing().when(statusListServiceAdapter).updateStatus(issuance);
            issuanceService.revokeIssuance("sessionId");
            Mockito.verify(issuanceAdapter).getRevocable(anyString());
            Mockito.verify(issuanceAdapter).revoke(issuance);
        }

        @Test
        void shouldErrorWhenIdentificationFailed() {
            doReturn(Optional.empty()).when(identificationApi).validateSessionAndGetPseudonym(anyString());
            assertThrows(IdentificationFailedException.class, () -> issuanceService.revokeIssuance("sessionId"));
        }
    }
}