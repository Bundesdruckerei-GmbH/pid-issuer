/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import de.bdr.pidi.end2end.requests.SessionRequestBuilder;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

class B1SessionRequestTest extends RemoteTest {
    @Test
    @DisplayName("Session request, happy path, variant b1")
    @Requirement({"PIDI-838","PIDI-1509"})
    @XrayTest(key = "PIDI-1762")
    void test001() {
        SessionRequestBuilder.valid()
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.OK)
                .body("session_id", is(notNullValue()))
                .body("session_id", matchesPattern(TestUtils.NONCE_REGEX))
                .body("session_id_expires_in", isA(Integer.class))
                .body("session_id_expires_in", greaterThan(0))
                .body("session_id_expires_in", is(60))
        ;
    }

    @Test
    @DisplayName("Session request invalid flow variant, variant b1")
    @Requirement({"PIDI-838","PIDI-1509"})
    @XrayTest(key = "PIDI-1769")
    void test002() {
        SessionRequestBuilder.valid().withUrl("/b/session")
                .doRequest()
                .then()
                .assertThat()
                .status(HttpStatus.NOT_FOUND)
                .body("session_id", is(nullValue()))
                .body("session_id_expires_in", is(nullValue()))
                .body("error", is("Not Found"))
        ;
    }
}
