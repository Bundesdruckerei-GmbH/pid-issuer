/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.requests;

public class MockBehaviorRequestBuilder extends EidMockRequestBuilder<MockBehaviorRequestBuilder> {
    public static enum Behavior {
        SUCCESS, STATUS_INTERNAL_ERROR, OUTDATED_DOCUMENT
    }

    private Behavior behavior = Behavior.SUCCESS;

    public MockBehaviorRequestBuilder withBehavior(Behavior behavior) {
        this.behavior = behavior;
        withUrl(createUrl());
        return this;
    }

    @Override
    String getPath() {
        return "behaviour/" + behavior;
    }


}
