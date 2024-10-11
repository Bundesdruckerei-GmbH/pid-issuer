/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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

public abstract class RequestBuilder<SELF extends RequestBuilder<?>> {
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

    public SELF withWebTestClient(WebTestClient webTestClient) {
        this.webTestClient = webTestClient;
        return (SELF) this;
    }
    public SELF withLogging(boolean logging) {
        this.logging = logging;
        return (SELF) this;
    }

    public SELF withFilter(Filter filter) {
        filters.add(filter);
        return (SELF) this;
    }

    public SELF withHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return (SELF) this;
    }

    public SELF withHeader(String header, String value) {
        List<String> valueList = value == null ? List.of() : List.of(value);
        headerParameters.put(header, valueList);
        return (SELF) this;
    }

    public SELF withHeaders(String header, String... values) {
        headerParameters.put(header,List.of(values));
        return (SELF)this;
    }

    public SELF withContentType(String contentType) {
        return withHeader("Content-Type", contentType);
    }

    public SELF withQueryParam(String key, String value) {
        queryParameters.put(key, List.of(value));
        return (SELF) this;
    }

    public SELF withoutQueryParam(String key) {
        queryParameters.remove(key);
        return (SELF) this;
    }

    public SELF withQueryParam(String key, String... values) {
        queryParameters.put(key, List.of(values));
        return (SELF) this;
    }

    public SELF withFormParam(String key, String value) {
        formParameters.put(key, List.of(value));
        return (SELF) this;
    }

    public SELF withoutFormParam(String key) {
        formParameters.remove(key);
        return (SELF) this;
    }

    public SELF withFormParam(String key, String... values) {
        formParameters.put(key, List.of(values));
        return (SELF) this;
    }
    public SELF withoutHeader(String key) {
        headerParameters.remove(key);
        return (SELF) this;
    }
    public SELF withUrl(String url) {
        this.url = url;
        return (SELF) this;
    }

    public SELF withJsonBody(ObjectNode requestBody) {
        if (this.requestBody != null) {
            this.requestBody.setAll(requestBody);
        } else {
            this.requestBody = requestBody;
        }
        return (SELF) this;
    }

    public SELF withRemovedJsonBodyProperty(String property) {
        if (this.requestBody != null) {
            this.requestBody.remove(property);
            return (SELF) this;
        } else {
            throw new IllegalStateException("No jsonBody to remove property from");
        }
    }

    public SELF withoutJsonBody() {
        this.requestBody = null;
        return (SELF) this;
    }

    public SELF withAccessToken(String token) {
        withHeader("Authorization", "%s %s".formatted(TokenType.DPOP.getValue(), token));
        return (SELF) this;
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
    public SELF withDocumentation(String name) {
        this.document = document(name);
        return (SELF) this;
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
    public SELF withDocumentation(Documentation documentation) {
        if (documentation != null) {
            this.document = document(documentation.name());
        } else {
            this.document = null;
        }
        return (SELF) this;
    }

    public String getRequestUrl(String host) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(host).path(url);
        for (Map.Entry<String, Collection<String>> entry : queryParameters.entrySet()) {
            uriComponentsBuilder.queryParam(entry.getKey(), entry.getValue());
        }
        return uriComponentsBuilder.build().toUriString();
    }
}
