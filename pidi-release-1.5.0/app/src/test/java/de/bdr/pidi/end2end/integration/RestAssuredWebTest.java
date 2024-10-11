/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.integration;

import de.bdr.pidi.end2end.requests.RequestBuilder;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base class for {@link io.restassured.RestAssured} based tests. These test are will be executed parallel by junit.
 * <p>
 * Because of problems with spring rest docs in concurrent execution calling {@link RequestBuilder#withDocumentation(String)} will lead to an error.
 */
public abstract class RestAssuredWebTest extends IntegrationTest {
    @BeforeEach
    void setUpWithoutRestDoc(WebApplicationContext applicationContext) {
        MockMvcWebTestClient.MockMvcServerSpec<?> mockMvcServerSpec = MockMvcWebTestClient.bindToApplicationContext(applicationContext);

        WebTestClient webTestClient = mockMvcServerSpec.build();
        RestAssuredWebTestClient.webTestClient(webTestClient);
    }
}
