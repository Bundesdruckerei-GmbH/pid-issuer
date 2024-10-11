/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SessionKeyTest {
    @Test
    @DisplayName("Verify no duplicate keys")
    void test001() {
        var keys = Arrays.stream(SessionKey.values()).map(SessionKey::getValue).collect(Collectors.toSet());
        assertThat(keys).hasSameSizeAs(SessionKey.values());
    }
}
