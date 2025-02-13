/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.requests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bdr.pidi.eidauthmock.client.model.EidAuthMockEidData;

public class MockIdRequestBuilder extends EidMockRequestBuilder<MockIdRequestBuilder> {
    public static final String MOCK_DEFAULT_PSEUDONYM = "RqmT-zkvHpUNEqmUXI7VkQOH-9FsMk789S1RrWrtYNw=";
    private String pseudonym = MOCK_DEFAULT_PSEUDONYM; // the default mock pseudonym

    public MockIdRequestBuilder withPseudonym(String pseudonym) {
        this.pseudonym = pseudonym;
        return withUrl(createUrl());
    }

    public MockIdRequestBuilder withFamilyName(String familyName) {
        return withQueryParam("familyName", familyName);

    }

    public MockIdRequestBuilder withRandomId() {
        return withOutFamilyName().withPseudonym("fixedRandom");
    }

    public MockIdRequestBuilder withData(EidAuthMockEidData data) {
        ObjectMapper objectMapper = new ObjectMapper();
        return withPseudonym("data")
                .withHeader("accept", "application/json")
                .withHeader("content-type", "application/json")
                .withJsonBody((com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.convertValue(data, JsonNode.class));
    }

    public MockIdRequestBuilder withOutFamilyName() {
        return withoutQueryParam("familyName");

    }

    @Override
    String getPath() {
        return "pseudonym/" + pseudonym;
    }
}
