/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.remote;

import de.bdr.pidi.end2end.requests.MockBehaviorRequestBuilder;
import de.bdr.pidi.testdata.TestConfig;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

@Slf4j
@Tag("remote")
@Tag("e2e")
public abstract class RemoteTest {

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException, KeyManagementException {

        String baseUrl = TestConfig.pidiBaseUrl();
        log.info("Base url: {}", baseUrl);

        WebTestClient webTestClient = WebTestClient
                .bindToServer(getInsecureHttpConnector())
                .baseUrl(baseUrl)
                .responseTimeout(Duration.of(5, SECONDS))
                .build();

        RestAssuredWebTestClient.webTestClient(webTestClient);
    }

    @BeforeEach
    void resetMock() {
        String newBehavior = new MockBehaviorRequestBuilder().withBehavior(MockBehaviorRequestBuilder.Behavior.SUCCESS).withHost(TestConfig.getEidMockHostname()).doRequest().body().asString();
        log.debug("New behavior: {}", newBehavior);
    }

    private ClientHttpConnector getInsecureHttpConnector() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[]{new InsecureTrustManager()}, new SecureRandom());
        return new JdkClientHttpConnector(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .sslContext(sslContext)
                .build());
    }
}
