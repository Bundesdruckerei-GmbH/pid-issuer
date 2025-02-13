/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance;

import de.bdr.revocation.issuance.adapter.out.rest.api.DefaultApi;
import de.bdr.revocation.issuance.adapter.out.rest.api.model.References;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTest {
    protected static final String STATUS_LIST_POOL_ID = "verified-email";

    @Autowired
    protected DefaultApi statusListServiceClient;

    protected References newReferences(String id, Integer amount) {
        return statusListServiceClient.newReferences(id, amount);
    }
}
