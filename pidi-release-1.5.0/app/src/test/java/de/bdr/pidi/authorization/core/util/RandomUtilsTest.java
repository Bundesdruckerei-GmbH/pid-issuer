/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class RandomUtilsTest {
    @DisplayName("Verify random string length")
    @Test
    void test001() {
        assertThat(RandomUtil.randomString(), hasLength(22));
    }

    @DisplayName("Verify two random strings differ")
    @Test
    void test002() {
        assertThat(RandomUtil.randomString(), is(not(RandomUtil.randomString())));
    }

    @DisplayName("validate strings are randoms")
    @Test
    void test003() {
        assertThat(RandomUtil.isValid("123"), is(false));
        assertThat(RandomUtil.isValid(RandomUtil.randomString()), is(true));
    }
}
