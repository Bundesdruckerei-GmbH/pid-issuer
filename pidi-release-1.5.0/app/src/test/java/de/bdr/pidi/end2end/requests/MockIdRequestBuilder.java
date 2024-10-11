/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.requests;

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

    public MockIdRequestBuilder withOutFamilyName() {
        return withoutQueryParam("familyName");

    }

    @Override
    String getPath() {
        return "pseudonym/" + pseudonym;
    }
}
