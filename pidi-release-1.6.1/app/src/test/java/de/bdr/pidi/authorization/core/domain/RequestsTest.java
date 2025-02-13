/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.domain;

import de.bdr.pidi.authorization.FlowVariant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestsTest {

    @DisplayName("Validate pushed authorization path")
    @Test
    void test001() {
        assertThat(Requests.PUSHED_AUTHORIZATION_REQUEST.getPath()).isEqualTo("par");
        assertThat(Requests.PUSHED_AUTHORIZATION_REQUEST.getPath(FlowVariant.C)).isEqualTo("c/par");
    }

    @DisplayName("Validate authorization path")
    @Test
    void test002() {
        assertThat(Requests.AUTHORIZATION_REQUEST.getPath()).isEqualTo("authorize");
        assertThat(Requests.AUTHORIZATION_REQUEST.getPath(FlowVariant.C)).isEqualTo("c/authorize");
    }

    @DisplayName("Validate finish authorization path")
    @Test
    void test004() {
        assertThat(Requests.FINISH_AUTHORIZATION_REQUEST.getPath()).isEqualTo("finish-authorization");
        assertThat(Requests.FINISH_AUTHORIZATION_REQUEST.getPath(FlowVariant.C)).isEqualTo("c/finish-authorization");
    }

    @DisplayName("Validate token path")
    @Test
    void test005() {
        assertThat(Requests.TOKEN_REQUEST.getPath()).isEqualTo("token");
        assertThat(Requests.TOKEN_REQUEST.getPath(FlowVariant.C)).isEqualTo("c/token");
    }

    @DisplayName("Validate seed credential path")
    @Test
    void test006() {
        assertThat(Requests.SEED_CREDENTIAL_REQUEST.getPath()).isEqualTo("credential");
        assertThat(Requests.SEED_CREDENTIAL_REQUEST.getPath(FlowVariant.C)).isEqualTo("c/credential");
    }

    @DisplayName("Validate credential path")
    @Test
    void test007() {
        assertThat(Requests.CREDENTIAL_REQUEST.getPath()).isEqualTo("credential");
        assertThat(Requests.CREDENTIAL_REQUEST.getPath(FlowVariant.C)).isEqualTo("c/credential");
    }

    @DisplayName("Validate identification no path")
    @Test
    void test008() {
        assertThat(Requests.IDENTIFICATION_RESULT.getPath()).isNull();
        assertThat(Requests.IDENTIFICATION_RESULT.getPath(FlowVariant.C)).isNull();
    }
}