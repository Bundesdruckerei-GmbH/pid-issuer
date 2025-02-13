/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.remote;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.requests.FinishAuthorizationRequestBuilder;
import de.bdr.pidi.end2end.requests.MockBehaviorRequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import de.bdr.pidi.testdata.TestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertAll;

@Slf4j
@Isolated
class C1FinishAuthorizationMockModTest extends RemoteTest {

    private final Steps steps = new Steps(FlowVariant.C1);

    @DisplayName("Finish authorization happy path, outdated document, variant c1")
    @Test
    @Requirement({"PIDI-300"})
    void test007() {
        var mockResponse = new MockBehaviorRequestBuilder().withBehavior(MockBehaviorRequestBuilder.Behavior.OUTDATED_DOCUMENT).withHost(TestConfig.getEidMockHostname()).doRequest();
        String newBehavior = mockResponse.body().asString();
        log.debug("New behavior: {}", newBehavior);
        var requestUri = steps.doPAR();
        var issuerState = steps.doAuthorize(requestUri);
        var location = new FinishAuthorizationRequestBuilder("/c1/finish-authorization")
                .withIssuerState(issuerState)
                .doRequest()
                .then()
                .log().all()
                .assertThat()
                .status(HttpStatus.FOUND)
                .header("location", startsWith("https://secure.redirect.com?"))
                .header("dpop-nonce", is(nullValue()))
                .extract().header("Location");
        var uriComponents = UriComponentsBuilder.fromUriString(location).build();
        assertAll(
                () -> assertThat(uriComponents.getQueryParams().get("error"), hasSize(1)),
                () -> assertThat(uriComponents.getQueryParams().get("error").getFirst(), is("access_denied")),
                () -> assertThat(uriComponents.getQueryParams().get("error_description"), hasSize(1)),
                () -> assertThat(uriComponents.getQueryParams().get("error_description").getFirst(), is("Identification%20failed"))
        );
    }
}
