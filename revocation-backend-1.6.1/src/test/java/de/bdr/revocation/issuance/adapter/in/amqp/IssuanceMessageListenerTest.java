/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.adapter.in.amqp;

import de.bdr.revocation.issuance.TestUtils;
import de.bdr.revocation.issuance.adapter.in.amqp.IssuanceMessageListener;
import de.bdr.revocation.issuance.app.service.BusinessException;
import de.bdr.revocation.issuance.app.service.IssuanceService;
import java.util.Objects;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.util.SerializationUtils;

@ExtendWith(MockitoExtension.class)
class IssuanceMessageListenerTest {

    @Mock
    private IssuanceService issuanceService;

    @InjectMocks
    private IssuanceMessageListener listener;

    @Test
    void shouldSaveIssuance() throws BusinessException {
        // Given
        val issuance = TestUtils.createIssuance();
        val message = getMessage(issuance);

        // When
        listener.receiveMessage(message, issuance);

        // Then
        Mockito.verify(issuanceService).saveIssuance(issuance);
    }

    private static Message getMessage(Object payload) {
        var message = new Message(Objects.requireNonNull(SerializationUtils.serialize(payload)));
        message.getMessageProperties().setMessageId(UUID.randomUUID().toString());
        return message;
    }
}