/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.adapter.in.amqp;

import de.bdr.revocation.issuance.IntegrationTest;
import de.bdr.revocation.issuance.adapter.in.amqp.AmqpConfiguration;
import de.bdr.revocation.issuance.adapter.out.persistence.IssuanceEntity;
import de.bdr.revocation.issuance.adapter.out.persistence.IssuanceRepository;
import de.bdr.revocation.issuance.app.domain.Issuance;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.beans.factory.annotation.Autowired;

import static de.bdr.revocation.issuance.TestUtils.createIssuance;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RabbitListenerTest
class IssuanceMessageListenerITest extends IntegrationTest {
    private static final Duration AWAIT_AT_MOST = Duration.ofSeconds(5L);
    private static final Duration AWAIT_POLL_DELAY = Duration.ofMillis(500L);
    private static final String QUEUE_MESSAGE_COUNT = RabbitAdmin.QUEUE_MESSAGE_COUNT.toString();

    @Autowired
    private IssuanceRepository issuanceRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    private final List<Issuance> issuances = new ArrayList<>();

    @BeforeAll
    void setUp() {
        rabbitAdmin.purgeQueue(AmqpConfiguration.DL_QUEUE);
    }

    @AfterAll
    void afterAll() {
        issuances.forEach(i ->
                issuanceRepository.findByPseudonymAndListIdAndListIndex(i.getPseudonym(), i.getListID(), i.getIndex())
                        .ifPresent(entity -> issuanceRepository.delete(entity)));
        rabbitAdmin.purgeQueue(AmqpConfiguration.ISSUANCE_QUEUE);
        rabbitAdmin.purgeQueue(AmqpConfiguration.DL_QUEUE);
    }

    @Test
    void shouldSaveIssuance() {
        // Given
        val issuance = createIssuance("test1", "listId1");
        issuances.add(issuance);
        sendMessage(issuance);

        // Then
        awaitEntries(matchingFields(issuance), 1);
    }

    @Test
    void shouldUpdateIssuance() {
        // Given
        val issuance = createIssuance("test2", "listId2");
        issuances.add(issuance);
        sendMessage(issuance);

        // Then
        awaitEntries(matchingFields(issuance), 1);

        // Given
        issuance.setExpirationTime(issuance.getExpirationTime().plusSeconds(60L));
        sendMessage(issuance);

        // Then
        awaitEntryAfterTime(matchingExpirationTime(issuance), 1);
        awaitEntries(matchingFields(issuance), 1); // is not 2
    }

    @Test
    void shouldRejectInvalidIssuance() {
        // Given
        val issuance = createIssuance("test3");
        issuance.setListID(null);
        sendMessage(issuance);

        // Then
        awaitDeadletterQueueEntries(1);
        rabbitAdmin.purgeQueue(AmqpConfiguration.DL_QUEUE);
    }

    @Test
    void shouldRejectIssuanceViolatingConstraint() {
        // Given
        val issuance1 = createIssuance("test4.1", "listId4");
        issuances.add(issuance1);
        sendMessage(issuance1);

        // Then
        awaitEntries(matchingFields(issuance1), 1);

        // Given
        val issuance2 = createIssuance("test4.2", "listId4");
        issuance2.setIndex(issuance1.getIndex());
        sendMessage(issuance2);

        // Then
        awaitDeadletterQueueEntries(1);
        rabbitAdmin.purgeQueue(AmqpConfiguration.DL_QUEUE);
    }

    private void sendMessage(Issuance issuance) {
        rabbitTemplate.convertAndSend(AmqpConfiguration.ISSUANCE_QUEUE, issuance, message -> {
            message.getMessageProperties().setMessageId(UUID.randomUUID().toString());
            return message;
        });
    }

    private long countMessages(String queueName) {
        return Optional.ofNullable(rabbitAdmin.getQueueProperties(queueName))
                .map(properties -> properties.get(QUEUE_MESSAGE_COUNT))
                .map(Object::toString).map(Long::parseLong).orElse(0L);
    }

    private Predicate<IssuanceEntity> matchingPseudonym(String pseudonym) {
        return entity -> Objects.equals(entity.getPseudonym(), pseudonym);
    }

    private Predicate<IssuanceEntity> matchingExpirationTime(Issuance issuance) {
        return matchingFields(issuance)
                .and(entity -> Objects.equals(entity.getExpirationTime(), issuance.getExpirationTime()));
    }

    private Predicate<IssuanceEntity> matchingFields(Issuance issuance) {
        return matchingPseudonym(issuance.getPseudonym())
                .and(entity -> Objects.equals(entity.getListId(), issuance.getListID()))
                .and(entity -> Objects.equals(entity.getListIndex(), issuance.getIndex()));
    }

    private void awaitDeadletterQueueEntries(long count) {
        await().atMost(AWAIT_AT_MOST).until(() ->
                countMessages(AmqpConfiguration.DL_QUEUE), is(count));
    }

    private void awaitEntries(Predicate<IssuanceEntity> matcher, long count) {
        await().pollInterval(AWAIT_POLL_DELAY).atMost(AWAIT_AT_MOST).until(() ->
                issuanceRepository.findAll().stream().filter(matcher).count(), is(count));
    }

    private void awaitEntryAfterTime(Predicate<IssuanceEntity> matcher, long count) {
        await().pollDelay(AWAIT_POLL_DELAY).pollInterval(AWAIT_POLL_DELAY).atMost(AWAIT_AT_MOST).until(() ->
                issuanceRepository.findAll().stream().filter(matcher).count(), is(count));
    }
}
