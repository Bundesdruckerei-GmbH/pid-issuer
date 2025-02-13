/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.adapter.in.rest;

import de.bdr.revocation.identification.adapter.out.persistence.AuthenticationEntity;
import de.bdr.revocation.identification.adapter.out.persistence.AuthenticationRepository;
import de.bdr.revocation.identification.core.model.Authentication;
import de.bdr.revocation.issuance.IntegrationTest;
import de.bdr.revocation.issuance.TestUtils;
import de.bdr.revocation.issuance.adapter.out.persistence.IssuanceEntity;
import de.bdr.revocation.issuance.adapter.out.persistence.IssuanceRepository;
import de.bdr.revocation.issuance.adapter.out.rest.api.model.Reference;
import de.bdr.revocation.issuance.adapter.out.rest.api.model.References;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UsersControllerITest extends IntegrationTest {

    private static final int COUNT_REFERENCES = 3;
    private static final String PSEUDONYM = "pseudo";
    private static final String HEADER_X_SESSION_ID = "X-Session-ID";

    @Autowired
    private IssuanceRepository issuanceRepository;

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @LocalServerPort
    private int port;

    private List<Reference> reservedReferences;
    private List<IssuanceEntity> issuanceEntities = new ArrayList<>();

    @BeforeAll
    void beforeAll() {
        References references = newReferences(STATUS_LIST_POOL_ID, COUNT_REFERENCES);
        reservedReferences = references.getReferences();
        assertThat(reservedReferences).hasSize(COUNT_REFERENCES);
    }

    @BeforeEach
    void setUp() {
        issuanceEntities = List.of(
                issuanceRepository.save(TestUtils.createIssuanceEntity(PSEUDONYM, reservedReferences.get(0), false)),
                issuanceRepository.save(TestUtils.createIssuanceEntity(PSEUDONYM, reservedReferences.get(1), false)),
                issuanceRepository.save(TestUtils.createIssuanceEntity(PSEUDONYM, reservedReferences.get(2), true))
        );
    }

    @AfterEach
    void tearDown() {
        issuanceRepository.deleteAll(issuanceEntities);
        authenticationRepository.deleteAll();
    }

    @DisplayName("should successfully get issuance count")
    @Test
    void test001() {
        String sessionId = UUID.randomUUID().toString();
        authenticationRepository.save(new AuthenticationEntity(newAuthentication(sessionId, PSEUDONYM)));

        given()
                .port(port)
                .header(HEADER_X_SESSION_ID, sessionId)
        .when()
                .get("/users/issuances/count")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("issued", is(3))
                .body("revocable", is(2));
    }

    @DisplayName("should get zero issuance count for unknown pseudonym")
    @Test
    void test002() {
        String sessionId = UUID.randomUUID().toString();
        authenticationRepository.save(new AuthenticationEntity(newAuthentication(sessionId, "unknown")));

        given()
                .port(port)
                .header(HEADER_X_SESSION_ID, sessionId)
        .when()
                .get("/users/issuances/count")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("issued", is(0))
                .body("revocable", is(0));
    }

    @DisplayName("should successfully revoke PIDs")
    @Test
    void test003() {
        String sessionId = UUID.randomUUID().toString();
        authenticationRepository.save(new AuthenticationEntity(newAuthentication(sessionId, PSEUDONYM)));
        given()
                .port(port)
                .header(HEADER_X_SESSION_ID, sessionId)
        .when()
                .delete("/users/issuances")
        .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        issuanceEntities.forEach(e -> {
            var e1 = issuanceRepository.findById(e.getId());
            assertThat(e1).get().extracting(IssuanceEntity::isRevoked).isEqualTo(true);
        });
    }

    @DisplayName("should revoke nothing for unknown pseudonym")
    @Test
    void test004() {
        String sessionId = UUID.randomUUID().toString();
        authenticationRepository.save(new AuthenticationEntity(newAuthentication(sessionId, "unknown")));
        given()
                .port(port)
                .header(HEADER_X_SESSION_ID, sessionId)
        .when()
                .delete("/users/issuances")
        .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        issuanceEntities.forEach(e -> {
            var e1 = issuanceRepository.findById(e.getId());
            assertThat(e1).get().extracting(IssuanceEntity::isRevoked).isEqualTo(e.isRevoked());
        });
    }

    @DisplayName("should handle error if listid and/or index is unknown")
    @Test
    void test005() {
        String sessionId = UUID.randomUUID().toString();
        authenticationRepository.save(new AuthenticationEntity(newAuthentication(sessionId, PSEUDONYM)));
        val reference = new Reference().uri("unknown").index(88);
        IssuanceEntity entity = null;
        try {
            entity = issuanceRepository.save(TestUtils.createIssuanceEntity(PSEUDONYM, reference, false));
            given()
                    .port(port)
                    .header(HEADER_X_SESSION_ID, sessionId)
            .when()
                    .delete("/users/issuances")
            .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body("message", is("Some issuances could not be revoked."));
        } finally {
            if (entity != null) {
                issuanceRepository.delete(entity);
            }
        }
    }

    @DisplayName("should unauthorized when session invalid")
    @Test
    void test006() {
        String sessionId = UUID.randomUUID().toString();

        given()
                .port(port)
                .header(HEADER_X_SESSION_ID, sessionId)
                .when()
                .get("/users/issuances/count")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    private Authentication newAuthentication(String sessionID, String pseudonym) {
        Instant now = Instant.now();
        val uuid = UUID.randomUUID().toString();
        return Authentication.restoreAuthenticated(sessionID, uuid, uuid, uuid, pseudonym, now.plusSeconds(600L), now);
    }
}
