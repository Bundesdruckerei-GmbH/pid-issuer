/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.in;

import de.bdr.pidi.authorization.core.util.RandomUtil;
import de.bdr.pidi.end2end.integration.RestAssuredWebTest;
import de.bdr.pidi.identification.core.model.AuthenticationState;
import de.bdr.pidi.identification.out.persistence.AuthenticationEntity;
import de.bdr.pidi.identification.out.persistence.AuthenticationRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
    private final List<Long> authenticationIds = new ArrayList<>();

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @LocalManagementPort
    private int port;

    @AfterEach
    void tearDown() {
        authenticationRepository.deleteAllById(authenticationIds);
        authenticationIds.clear();
    }

    @DisplayName("Cleanup expired authentication on identification housekeeping")
    @ParameterizedTest
    @EnumSource(AuthenticationState.class)
    void test001(AuthenticationState state) {
        createExpiredAuthentication(state, expired);

        given()
                .when()
                .port(port)
                .contentType(ContentType.JSON)
                .post("/actuator/housekeeping/identification")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

        checkAuthenticationEmpty();
    }

    @DisplayName("Skip valid authentication on identification housekeeping")
    @ParameterizedTest
    @EnumSource(value = AuthenticationState.class, names = {"TERMINATED", "TIMEOUT"}, mode = EnumSource.Mode.EXCLUDE)
    void test002(AuthenticationState state) {
        createExpiredAuthentication(state, valid);

        given()
                .when()
                .port(port)
                .contentType(ContentType.JSON)
                .post("/actuator/housekeeping/identification")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

        checkAuthenticationPresent();
    }

    @DisplayName("Cleanup authentication in final state on identification housekeeping")
    @ParameterizedTest
    @EnumSource(value = AuthenticationState.class, names = {"TERMINATED", "TIMEOUT"}, mode = EnumSource.Mode.INCLUDE)
    void test003(AuthenticationState state) {
        createExpiredAuthentication(state, valid);

        given()
                .when()
                .port(port)
                .contentType(ContentType.JSON)
                .post("/actuator/housekeeping/identification")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

        checkAuthenticationEmpty();
    }

    void createExpiredAuthentication(AuthenticationState state, Instant expires) {
        var auth = new AuthenticationEntity();
        auth.setAuthenticationState(state);
        auth.setSessionId(RandomUtil.randomString());
        auth.setTokenId(RandomUtil.randomString());
        auth.setCreated(Instant.now().minus(Duration.ofDays(14)));
        auth.setValidUntil(expires);
        var entity = authenticationRepository.save(auth);
        authenticationIds.add(entity.getId());
        checkAuthenticationPresent();
    }

    void checkAuthenticationEmpty() {
        assertThat(authenticationRepository.findAllById(authenticationIds)).isEmpty();
    }

    void checkAuthenticationPresent() {
        assertThat(authenticationRepository.findAllById(authenticationIds)).hasSize(1);
    }
}
