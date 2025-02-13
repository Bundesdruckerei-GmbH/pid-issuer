/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.out.revoc;

import de.bdr.openid4vc.vci.service.statuslist.StatusReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@RabbitListenerTest
@Import({RabbitTestConfig.class})
class RevocationAdapterITest {

    @MockitoSpyBean(name = "rabbitTemplate")
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RevocationAdapter adapter;

    @Autowired
    private RabbitListenerTestHarness harness;

    @AfterEach
    void tearDown() {
        reset(rabbitTemplate);
        reset(harness.<RabbitTestConfig.Listener>getSpy("test"));
    }

    @DisplayName("should send issuance message")
    @Test
    void test001() throws Exception {
        // given
        RabbitTestConfig.Listener listener = harness.getSpy("test");
        var answer = harness.getLatchAnswerFor("test", 1);
        doAnswer(answer).when(listener).testQueue(anyString());

        // when
        adapter.notifyRevocService(
                "pseudo",
                new StatusReference("http://test/647e8cdd-1ef0-4cf8-b0ed-2f436d8811f9", 1),
                Instant.parse("2024-11-04T16:00:00Z")
        );

        // then
        answer.await(5);
        verify(listener).testQueue("""
                {"pseudonym":"pseudo",\
                "listID":"http://test/647e8cdd-1ef0-4cf8-b0ed-2f436d8811f9",\
                "index":1,\
                "expirationTime":"2024-11-04T16:00:00Z"}""");
    }

    @DisplayName("should retry on exception")
    @Test
    void test002() throws InterruptedException {
        // given
        doThrow(AmqpException.class).doCallRealMethod()
                .when(rabbitTemplate).convertAndSend(eq("provideIssuanceInformation"), any(Object.class));
        RabbitTestConfig.Listener listener = harness.getSpy("test");
        var answer = harness.getLatchAnswerFor("test", 1);
        doAnswer(answer).when(listener).testQueue(anyString());

        // when
        adapter.notifyRevocService(
                "pseudo",
                new StatusReference("http://test/647e8cdd-1ef0-4cf8-b0ed-2f436d8811f9", 2),
                Instant.parse("2024-11-04T16:00:00Z")
        );

        // then
        answer.await(5);
        verify(listener).testQueue(anyString());
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), any(Object.class));
    }
}