/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.adapter.in.amqp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Slf4j
@Configuration
@EnableRetry
public class AmqpConfiguration {
    public static final String ISSUANCE_DIRECT_EXCHANGE = "pidi.direct";

    public static final String ISSUANCE_QUEUE = "revocation-service.issuance-provided";
    public static final String DL_QUEUE = "revocation-service.issuance-provided.dlq";
    public static final String ISSUANCE_ROUTING_KEY = "provideIssuanceInformation";

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(ISSUANCE_DIRECT_EXCHANGE);
    }

    @Bean
    public Queue deadletterQueue() {
        return QueueBuilder.durable(DL_QUEUE).quorum().build();
    }

    @Bean
    public Queue issuanceQueue() {
        return QueueBuilder.durable(ISSUANCE_QUEUE).quorum()
                .withArgument("x-dead-letter-exchange", "")
                .deadLetterRoutingKey(DL_QUEUE)
                .build();
    }

    @Bean
    public Binding issuanceBinding(Queue issuanceQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(issuanceQueue).to(directExchange).with(ISSUANCE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
