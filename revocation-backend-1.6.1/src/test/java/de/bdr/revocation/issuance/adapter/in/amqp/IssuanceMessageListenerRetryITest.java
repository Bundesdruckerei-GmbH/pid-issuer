/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.adapter.in.amqp;

import de.bdr.revocation.issuance.adapter.out.persistence.IssuanceAdapter;
import de.bdr.revocation.issuance.adapter.out.persistence.IssuanceMapper;
import de.bdr.revocation.issuance.adapter.out.persistence.IssuanceRepository;
import de.bdr.revocation.issuance.app.domain.Issuance;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.creation.MockSettingsImpl;
import org.mockito.internal.stubbing.answers.DoesNothing;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static de.bdr.revocation.issuance.TestUtils.createIssuance;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RabbitListenerTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {IssuanceMessageListenerRetryITest.TestConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext
@Disabled("works only standalone, not together with other integration tests")
class IssuanceMessageListenerRetryITest {
    private static final Duration AWAIT_AT_MOST = Duration.ofSeconds(5L);
    private static final Duration AWAIT_POLL_DELAY = Duration.ofMillis(500L);
    private static final String QUEUE_MESSAGE_COUNT = RabbitAdmin.QUEUE_MESSAGE_COUNT.toString();

    @TestConfiguration
    @EnableRetry
    public static class TestConfig {
        @Bean
        @Primary
        public IssuanceAdapter issuanceAdapter(IssuanceRepository issuanceRepository, IssuanceMapper issuanceMapper) {
            return Mockito.mock(IssuanceAdapter.class, new MockSettingsImpl<>()
//                    .useConstructor(issuanceRepository, issuanceMapper)
                    .defaultAnswer(DoesNothing.doesNothing())
                    .verboseLogging());
        }
    }

    @Autowired
    private IssuanceAdapter adapter;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @BeforeEach
    void init() {
        Mockito.reset(adapter);
    }

    @Test
    void shouldRejectIssuanceAfterThreeTriesIfDatabaseIsWrong() {
        try {
            // Given
            val issuance = createIssuance("test5", "listId5");
            doThrow(RuntimeException.class).doThrow(RuntimeException.class).doThrow(RuntimeException.class).when(adapter).save(issuance);
            sendMessage(issuance);

            // Then
            awaitForEmptyQueue();
            verify(adapter, times(3)).save(issuance);
        } finally {
            rabbitAdmin.purgeQueue(AmqpConfiguration.DL_QUEUE);
        }
    }

    @Test
    void shouldSaveIssuanceIfDatabaseWasWrongForShortTime() {
        // Given
        val issuance = createIssuance("test6", "listId6");
        doThrow(RuntimeException.class).doAnswer(DoesNothing.doesNothing()).when(adapter).save(issuance);
        sendMessage(issuance);

        // Then
        awaitForEmptyQueue();
        verify(adapter, times(2)).save(issuance);
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

    private void awaitForEmptyQueue() {
        await().pollDelay(AWAIT_POLL_DELAY).atMost(AWAIT_AT_MOST).until(() ->
                countMessages(AmqpConfiguration.ISSUANCE_QUEUE), is(0L));
    }
}
