/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.core;

import de.bdr.pidi.authorization.FlowVariant;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Setter
@Getter
@ToString
@Validated
@Configuration
@ConfigurationProperties(prefix = "pidi.statuslistservice")
public class StatusListServiceConfiguration {

    @NotNull
    private String baseUrl;

    @Getter(AccessLevel.NONE)
    private Map<FlowVariant, String> apiKey;

    @Getter(AccessLevel.NONE)
    private Map<FlowVariant, String> poolId;

    public String getApiKey(@NotNull FlowVariant flowVariant) {
        return apiKey.get(flowVariant);
    }

    public String getPoolId(@NotNull FlowVariant flowVariant) {
        return poolId.get(flowVariant);
    }
}
