/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.adapter.in.amqp;

import de.bdr.revocation.issuance.app.domain.Issuance;
import de.bdr.revocation.issuance.app.service.BusinessException;
import de.bdr.revocation.issuance.app.service.IssuanceService;
import de.bdr.revocation.issuance.app.service.RevocationServerException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class IssuanceMessageListener {
    private final IssuanceService issuanceService;

    /**
     * RabbitMQ listen on queue {@value AmqpConfiguration#ISSUANCE_QUEUE}. Receive and process issuance-messages.
     */
    @RabbitListener(id = "issuanceMessageListener", queues = AmqpConfiguration.ISSUANCE_QUEUE, messageConverter = "jsonMessageConverter")
    @Retryable(maxAttemptsExpression = "${issuance.retry.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${issuance.retry.delay:100}"), retryFor = RevocationServerException.class)
    public void receiveMessage(Message message, @Payload @Valid Issuance issuance) throws BusinessException {
        log.info("Received issuance: {} with messageId {}", issuance, message.getMessageProperties().getMessageId());
        issuanceService.saveIssuance(issuance);
    }
}
