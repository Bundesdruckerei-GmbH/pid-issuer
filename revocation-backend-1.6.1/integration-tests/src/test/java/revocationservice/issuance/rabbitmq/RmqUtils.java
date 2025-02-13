/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package revocationservice.issuance.rabbitmq;


import java.io.IOException;
import java.time.Instant;

public class RmqUtils {

    private final KarateRmqProducer producer;

    public RmqUtils(String queueName, String rabbitMqUsername, String rabbitMqPassword, String rabbitMqHost, Integer rabbitMqPort) {
        producer = new KarateRmqProducer(queueName, rabbitMqUsername, rabbitMqPassword, rabbitMqHost, rabbitMqPort);
    }

    public void sendIssuance(String pseudonym, String listID, int index) throws IOException {
        Issuance issuance = new Issuance();
        issuance.setPseudonym(pseudonym);
        issuance.setListID(listID);
        issuance.setIndex(index);
        issuance.setExpirationTime(Instant.now().plusSeconds(600L));
        producer.putIssuanceMessage(issuance);
    }

}