/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.out.persistence;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.end2end.integration.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class PidiSessionRepositoryTest extends IntegrationTest {
    @Autowired
    private PidiSessionRepository pidiSessionRepositoryUT;

    @Test
    @DisplayName("Verify no entity found")
    void test001() {
        var entity = pidiSessionRepositoryUT.findById(-1L);
        assertThat(entity.isEmpty(), is(true));
    }

    @Test
    @DisplayName("Verify new entity found")
    void test002() {
        var session = new PidiSessionEntity();
        session.setFlow(FlowVariant.C1);
        session.setAuthorizationCode("authCode");
        session.setIssuerState("issuerState");
        session.setRequestUri("requestUri");
        session.setAccessToken("accessToken");
        session.setSession("sessionData");
        session.setExpires(Instant.now().plusSeconds(60));
        session.setNextExpectedRequest(Requests.AUTHORIZATION_REQUEST);

        var saved = pidiSessionRepositoryUT.save(session);

        assertThat(saved.getId(), is(notNullValue()));
    }
}
