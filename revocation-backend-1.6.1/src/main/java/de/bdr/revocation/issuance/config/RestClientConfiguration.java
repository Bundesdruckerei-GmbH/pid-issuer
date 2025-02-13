/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance.config;

import de.bdr.revocation.issuance.adapter.out.rest.ApiClient;
import de.bdr.revocation.issuance.adapter.out.rest.api.DefaultApi;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "statuslistservice.restclient")
public class RestClientConfiguration {

    @NotNull
    private String apiBasePath;

    @NotNull
    private String apiKey;

    @Bean
    public ApiClient apiClient(RestClient.Builder builder) {
        var apiClient = new ApiClient(builder.build());
        apiClient.setBasePath(apiBasePath);
        apiClient.setApiKey(apiKey);
        return apiClient;
    }

    @Bean
    public DefaultApi statusListServiceClient(ApiClient apiClient) {
        return new DefaultApi(apiClient);
    }
}
