/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.adapter.out.rest;

import de.bdr.revocation.issuance.TestUtils;
import de.bdr.revocation.issuance.adapter.out.rest.api.DefaultApi;
import de.bdr.revocation.issuance.adapter.out.rest.api.model.UpdateStatusRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class StatusListServiceAdapterTest {

    @Mock
    DefaultApi statusListServiceClient;

    @InjectMocks
    StatusListServiceAdapter statusListServiceAdapter;

    @Test
    void itWorks() {
        assertDoesNotThrow(() -> statusListServiceAdapter.updateStatus(TestUtils.createIssuance()));
    }

    @Test
    void itThrowsOnInvalidParameters() {
        Mockito.doThrow(RestClientResponseException.class).when(statusListServiceClient).updateStatus(Mockito.any(UpdateStatusRequest.class));
        var issuance = TestUtils.createIssuance();
        assertThrows(RestClientResponseException.class, () -> statusListServiceAdapter.updateStatus(issuance));
    }
}