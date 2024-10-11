/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.requests;

import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import de.bdr.pidi.testdata.TestConfig;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

public class EidRequestBuilder extends RequestBuilder<EidRequestBuilder> {

    private int port = TestConfig.getEidMockPort();
    private String host = TestConfig.getEidMockHostname();

    public EidRequestBuilder() {
        this(null);
    }

    public EidRequestBuilder(RestDocumentationContextProvider restDocumentation) {
        super(HttpMethod.GET);
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

    public EidRequestBuilder withTCTokenUrl(String tcTokenUrl) {
        withQueryParam("tcTokenURL", URLEncoder.encode(tcTokenUrl, StandardCharsets.UTF_8));
        return this;
    }

    public EidRequestBuilder withPort(int port) {
        this.port = port;
        withUrl(createUrl());
        return this;
    }

    public EidRequestBuilder withHost(String host) {
        this.host = host;
        withUrl(createUrl());
        return this;
    }

    private String createUrl() {
        return "http://" + this.host + ":" + this.port + "/eID-Client";
    }
}
