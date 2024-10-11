/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.integration;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.out.persistence.PidiSessionEntity;
import de.bdr.pidi.authorization.out.persistence.PidiSessionRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
class HousekeepingTest extends RestAssuredWebTest {

    private final Instant expired = Instant.now().minus(Duration.ofDays(1));
    private final Instant valid = Instant.now().plus(Duration.ofDays(1));
    private final List<Long> sessionIds = new ArrayList<>();

    @Autowired
    private PidiSessionRepository sessionRepository;

    @LocalManagementPort
    private int port;

    @AfterEach
    void tearDown() {
        sessionRepository.deleteAllById(sessionIds);
        sessionIds.clear();
    }

    @DisplayName("Cleanup expired session on authorization housekeeping")
    @Test
    void test001() {
        createSession(expired);

        given()
                .when()
                .port(port)
                .contentType(ContentType.JSON)
                .post("/actuator/housekeeping/authorization")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

        checkSessionEmpty();
    }

    @DisplayName("Skip valid session on authorization housekeeping")
    @Test
    void test002() {
        createSession(valid);

        given()
                .when()
                .port(port)
                .contentType(ContentType.JSON)
                .post("/actuator/housekeeping/authorization")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

        checkSessionPresent();
    }

    void createSession(Instant expires) {
        var session = new PidiSessionEntity();
        session.setExpires(expires);
        session.setFlow(FlowVariant.C);
        session.setNextExpectedRequest(Requests.AUTHORIZATION_REQUEST);
        var entity = sessionRepository.save(session);
        sessionIds.add(entity.getId());
        checkSessionPresent();
    }

    void checkSessionEmpty() {
        assertThat(sessionRepository.findAllById(sessionIds)).isEmpty();
    }

    void checkSessionPresent() {
        assertThat(sessionRepository.findAllById(sessionIds)).hasSize(1);
    }
}
