/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core;

import de.bdr.pidi.authorization.FlowVariant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FlowVariantTest {

    @ParameterizedTest
    @EnumSource(FlowVariant.class)
    void testFromUrlPath(FlowVariant flowVariant) {
        Assertions.assertEquals(flowVariant, FlowVariant.fromUrlPath(flowVariant.urlPath));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid"})
    @NullAndEmptySource
    void testFromUrlPath(String path) {
        Assertions.assertNull(FlowVariant.fromUrlPath(path));
    }
}