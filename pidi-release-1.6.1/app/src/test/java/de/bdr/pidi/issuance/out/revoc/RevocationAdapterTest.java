/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.out.revoc;

import de.bdr.openid4vc.vci.service.statuslist.StatusReference;
import de.bdr.pidi.issuance.out.revoc.model.Issuance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RevocationAdapterTest {

    @Captor
    private ArgumentCaptor<Issuance> amqpMessage;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RevocationAdapter adapter;

    @DisplayName("should send issuance message")
    @Test
    void test001() {
        var expTime = Instant.now().plusSeconds(30);
        adapter.notifyRevocService(
                "pseudo",
                new StatusReference("http://test/647e8cdd-1ef0-4cf8-b0ed-2f436d8811f9", 1),
                expTime
        );

        verify(rabbitTemplate).convertAndSend(eq("provideIssuanceInformation"), amqpMessage.capture());
        var message = amqpMessage.getValue();
        assertThat(message.getPseudonym()).isEqualTo("pseudo");
        assertThat(message.getListID()).isEqualTo("http://test/647e8cdd-1ef0-4cf8-b0ed-2f436d8811f9");
        assertThat(message.getIndex()).isEqualTo(1);
        assertThat(message.getExpirationTime()).isEqualTo(expTime);
    }
}