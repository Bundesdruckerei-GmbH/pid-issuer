/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.out.sls;

import de.bdr.openid4vc.vci.service.statuslist.StatusReference;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.base.PidServerException;
import de.bdr.pidi.issuance.core.StatusListServiceConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class StatusListAdapterImpl implements StatusListAdapter {
    private static final String CREATE_REFERENCE_URL_TEMPLATE = "%s/pools/%s/new-references";
    private static final String API_KEY_HEADER = "x-api-key";

    private final StatusListServiceConfiguration configuration;
    private final RestTemplate restTemplate;

    @Autowired
    public StatusListAdapterImpl(RestTemplateBuilder restTemplateBuilder, StatusListServiceConfiguration statusListServiceConfiguration) {
        configuration = statusListServiceConfiguration;
        restTemplate = restTemplateBuilder.build();
    }

    public StatusReference acquireFreeIndex(FlowVariant flowVariant) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add(API_KEY_HEADER, configuration.getApiKey(flowVariant));
        HttpEntity<Void> request = new HttpEntity<>(null, headers);
        var createReferenceUrl =
                CREATE_REFERENCE_URL_TEMPLATE.formatted(configuration.getBaseUrl(), configuration.getPoolId(flowVariant));
        try {
            References responseObject = restTemplate.postForObject(createReferenceUrl, request, References.class);
            return Objects.requireNonNull(responseObject).references().getFirst();
        } catch (RestClientException | NullPointerException e) {
            throw new PidServerException("Could not acquire free index from status list service!", e);
        }
    }
}
