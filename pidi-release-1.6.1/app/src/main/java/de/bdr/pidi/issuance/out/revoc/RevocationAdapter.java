/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.out.revoc;

import de.bdr.openid4vc.vci.service.statuslist.StatusReference;
import de.bdr.pidi.issuance.out.revoc.model.Issuance;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RevocationAdapter {
    // app/docs/api/issuer-info-asyncapi.yml$channels.revocation.publish.operationId
    public static final String ROUTING_KEY = "provideIssuanceInformation";
    private final RabbitTemplate rabbitTemplate;

    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 500))
    public void notifyRevocService(String pseudonym, StatusReference statusRef, Instant expirationTime) {
        var message = new Issuance(pseudonym, statusRef.getUri(), statusRef.getIndex(), expirationTime);
        rabbitTemplate.convertAndSend(ROUTING_KEY, message);
    }
}
