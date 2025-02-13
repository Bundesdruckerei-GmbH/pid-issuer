/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.restdoc;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.end2end.remote.InsecureTrustManager;
import de.bdr.pidi.end2end.requests.MockBehaviorRequestBuilder;
import de.bdr.pidi.end2end.requests.RequestBuilder;
import de.bdr.pidi.end2end.steps.Steps;
import de.bdr.pidi.testdata.TestConfig;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

/**
 * Base class of tests writing spring rest doc snippets.
 * <p>
 * Creating documentation snippets by calling {@link RequestBuilder#withDocumentation(String)} is the purpose of these tests.
 * <p>
 * These tests don't run concurrent because of problems with spring rest doc. Use with care since it will slow down
 * build times.
 */
@Slf4j
@ExtendWith(RestDocumentationExtension.class)
@Isolated
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@Tag("restdocEid")
public abstract class RestDocTest {
    protected Steps steps;

    abstract FlowVariant flowVariant();

    @BeforeEach
    void setUpWithRestDock(RestDocumentationContextProvider restDocumentation) throws NoSuchAlgorithmException, KeyManagementException {
        this.steps = new Steps(flowVariant(), restDocumentation);
        String baseUrl = TestConfig.pidiBaseUrl();
        log.info("Base url: {}", baseUrl);
        
        var webTestClient = WebTestClient.bindToServer(getInsecureHttpConnector()).baseUrl(baseUrl).filter(documentationConfiguration(restDocumentation)).build();
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
        return new JdkClientHttpConnector(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).sslContext(sslContext).build());
    }
}
