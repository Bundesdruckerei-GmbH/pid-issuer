/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.requests;

public class MockBehaviorRequestBuilder extends EidMockRequestBuilder<MockBehaviorRequestBuilder> {
    public static enum Behavior {
        SUCCESS, STATUS_INTERNAL_ERROR, OUTDATED_DOCUMENT
    }

    private Behavior behavior = Behavior.SUCCESS;

    public MockBehaviorRequestBuilder withBeahvior(Behavior behavior) {
        this.behavior = behavior;
        withUrl(createUrl());
        return this;
    }

    @Override
    String getPath() {
        return "behaviour/" + behavior;
    }


}
