/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.out.revoc;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTestConfig {

    @Bean(name = "simpleConverter")
    public MessageConverter simpleMessageConverter() {
        return new SimpleMessageConverter();
    }

    @Bean
    public Listener listener() {
        return new Listener();
    }

    public static class Listener {
        @RabbitListener(id = "test",
                bindings = @QueueBinding(
                        value = @Queue(autoDelete = "true"),
                        exchange = @Exchange("pidi.direct"),
                        key = "provideIssuanceInformation"),
                messageConverter = "simpleConverter")
        void testQueue(String in) {
            // invocation tested by RabbitListenerTestHarness
        }
    }
}
