/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.requests;

import de.bdr.pidi.testdata.TestConfig;

import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.Duration;

import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

public abstract class EidMockRequestBuilder<SELF extends EidMockRequestBuilder<?>> extends RequestBuilder<SELF> {
    private int port = TestConfig.getEidMockPort();
    private String host = TestConfig.getEidMockHostname();

    public EidMockRequestBuilder() {
        this(null);
    }

    public EidMockRequestBuilder(RestDocumentationContextProvider restDocumentation) {
        super(HttpMethod.POST);
        withWebTestClient(getWebTestClient(restDocumentation));
        withUrl(createUrl());
    }

    private WebTestClient getWebTestClient(RestDocumentationContextProvider restDocumentation) {
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        WebTestClient.Builder builder = WebTestClient
                .bindToServer(
                        new HttpComponentsClientHttpConnector(HttpAsyncClients.custom()
                                .disableRedirectHandling()
                                .build()))
                .responseTimeout(Duration.ofMinutes(1))
                .uriBuilderFactory(uriBuilderFactory);
        if (restDocumentation != null) {
            builder.filter(documentationConfiguration(restDocumentation));
        }
        return builder.build();
    }

    public SELF withPort(int port) {
        this.port = port;
        return withUrl(createUrl());

    }

    public SELF withHost(String host) {
        this.host = host;
        return withUrl(createUrl());
    }


    protected String createUrl() {
        return "http://" + this.host + ":" + this.port + "/" + getPath();
    }

    abstract String getPath();
}
