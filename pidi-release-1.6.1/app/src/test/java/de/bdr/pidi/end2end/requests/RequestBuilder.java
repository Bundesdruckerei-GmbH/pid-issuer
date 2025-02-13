/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.requests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bdr.openid4vc.vci.data.TokenType;
import io.restassured.filter.Filter;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

public abstract class RequestBuilder<T extends RequestBuilder<?>> {
    public static final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Collection<String>> queryParameters = new HashMap<>();
    private final Map<String, Collection<String>> headerParameters = new HashMap<>();
    private final Map<String, Collection<String>> formParameters = new HashMap<>();
    private final List<Filter> filters = new ArrayList<>();
    private ObjectNode requestBody = null;
    private boolean logging = false;
    protected HttpMethod httpMethod;
    protected String url;
    private Consumer<EntityExchangeResult<byte[]>> document;
    private WebTestClient webTestClient;

    public RequestBuilder(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    protected Collection<String> getHeaders(String header) {
        return headerParameters.get(header);
    }

    public T withWebTestClient(WebTestClient webTestClient) {
        this.webTestClient = webTestClient;
        return (T) this;
    }
    public T withLogging(boolean logging) {
        this.logging = logging;
        return (T) this;
    }

    public T withFilter(Filter filter) {
        filters.add(filter);
        return (T) this;
    }

    public T withHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return (T) this;
    }

    public T withHeader(String header, String value) {
        List<String> valueList = value == null ? List.of() : List.of(value);
        headerParameters.put(header, valueList);
        return (T) this;
    }

    public T withHeaders(String header, String... values) {
        headerParameters.put(header,List.of(values));
        return (T)this;
    }

    public T withContentType(String contentType) {
        return withHeader("Content-Type", contentType);
    }

    public T withQueryParam(String key, String value) {
        queryParameters.put(key, List.of(value));
        return (T) this;
    }

    public T withoutQueryParam(String key) {
        queryParameters.remove(key);
        return (T) this;
    }

    public T withQueryParam(String key, String... values) {
        queryParameters.put(key, List.of(values));
        return (T) this;
    }

    public T withFormParam(String key, String value) {
        formParameters.put(key, List.of(value));
        return (T) this;
    }

    public T withoutFormParam(String key) {
        formParameters.remove(key);
        return (T) this;
    }

    public T withFormParam(String key, String... values) {
        formParameters.put(key, List.of(values));
        return (T) this;
    }
    public T withoutHeader(String key) {
        headerParameters.remove(key);
        return (T) this;
    }
    public T withUrl(String url) {
        this.url = url;
        return (T) this;
    }

    public T withJsonBody(ObjectNode requestBody) {
        if (this.requestBody != null) {
            this.requestBody.setAll(requestBody);
        } else {
            this.requestBody = requestBody;
        }
        return (T) this;
    }

    public T withRemovedJsonBodyProperty(String property) {
        if (this.requestBody != null) {
            this.requestBody.remove(property);
            return (T) this;
        } else {
            throw new IllegalStateException("No jsonBody to remove property from");
        }
    }

    public T withoutJsonBody() {
        this.requestBody = null;
        return (T) this;
    }

    public T withAccessToken(String token) {
        withHeader("Authorization", "%s %s".formatted(TokenType.DPOP.getValue(), token));
        return (T) this;
    }

    public WebTestClientResponse doRequest() {
        var requestSpecification = given();
        if (webTestClient != null) {
            requestSpecification.webTestClient(webTestClient);
        }
        if (logging) {
            requestSpecification.log().all(true);
        }
        for (Map.Entry<String, Collection<String>> entry : queryParameters.entrySet()) {
            if (entry.getValue() != null && entry.getValue().size() == 1) {
                requestSpecification.queryParam(entry.getKey(), entry.getValue().iterator().next());
            } else if (entry.getValue() != null) {
                requestSpecification.queryParam(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, Collection<String>> entry : headerParameters.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().forEach(value -> requestSpecification.header(entry.getKey(), value));
            }
        }

        if (!formParameters.isEmpty()) {
            StringBuilder body = new StringBuilder();
            for (Map.Entry<String, Collection<String>> entry : formParameters.entrySet()) {
                if (entry.getValue() != null) {
                    for (String value : entry.getValue()) {
                        if (!body.isEmpty()) {
                            body.append("&");
                        }
                        body.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                        body.append("=");
                        body.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                    }
                }

            }
            requestSpecification.body(body.toString());
        }

        if (requestBody != null) {
            requestSpecification.body(requestBody);
        }

        var requestSender = requestSpecification.when();
        if (this.document != null) {
            requestSender.consumeWith(this.document);
        }
        if (httpMethod == HttpMethod.GET) {
            return requestSender.get(this.url);
        } else if (httpMethod == HttpMethod.POST) {
            return requestSender.post(this.url);
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method: " + httpMethod);
        }
    }

    /**
     * Creates spring rest doc request and response documentation for the call.
     * <p>
     * Use only in @{@link de.bdr.pidi.end2end.restdoc.RestDocTest} or other test classes
     * annotated with @{@link org.junit.jupiter.api.parallel.Isolated} to avoid concurrency errors.
     *
     * @param name the name of the created snippet
     * @return itself for fluent api calls
     */
    public T withDocumentation(String name) {
        this.document = document(name);
        return (T) this;
    }

    /**
     * Creates spring rest doc request and response documentation for the call.
     * <p>
     * Use only in @{@link de.bdr.pidi.end2end.restdoc.RestDocTest} or other test classes
     * annotated with @{@link org.junit.jupiter.api.parallel.Isolated} to avoid concurrency errors.
     *
     * @param documentation the name of the created snippet
     * @return itself for fluent api calls
     */
    public T withDocumentation(Documentation documentation) {
        if (documentation != null) {
            this.document = document(documentation.name());
        } else {
            this.document = null;
        }
        return (T) this;
    }

    public String getRequestUrl(String host) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(host).path(url);
        for (Map.Entry<String, Collection<String>> entry : queryParameters.entrySet()) {
            uriComponentsBuilder.queryParam(entry.getKey(), entry.getValue());
        }
        return uriComponentsBuilder.build().toUriString();
    }
}
