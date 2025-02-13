/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package revocationservice.issuance.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class KarateRmqProducer {

    private static final Logger logger = LoggerFactory.getLogger(KarateRmqProducer.class);

    private final Channel channel;
    private final String queueName;
    private final ObjectMapper objectMapper;

    public KarateRmqProducer(String queueName, String rabbitMqUsername, String rabbitMqPassword, String rabbitMqHost, Integer rabbitMqPort) {
        this.queueName = queueName;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername(rabbitMqUsername);
            factory.setPassword(rabbitMqPassword);
            factory.setHost(rabbitMqHost);
            factory.setPort(rabbitMqPort);
            Connection connection = factory.newConnection();
            channel = connection.createChannel();
            logger.debug("init producer");
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void putIssuanceMessage(Issuance issuance) throws IOException {
        String issuanceString = objectMapper.writeValueAsString(issuance);
        channel.basicPublish("", queueName, null, issuanceString.getBytes());
        logger.debug("put issuance message: '{}'", issuanceString);
    }

}