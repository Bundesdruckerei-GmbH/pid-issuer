/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.out.sls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bdr.openid4vc.vci.service.statuslist.StatusReference;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.base.PidServerException;
import de.bdr.pidi.issuance.core.StatusListServiceConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

@RestClientTest({StatusListAdapterImpl.class, StatusListServiceConfiguration.class})
class StatusListAdapterImplTest {

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private StatusListAdapter statusListAdapter;

    @Autowired
    private StatusListServiceConfiguration configuration;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetReference() throws JsonProcessingException {
        // Given
        var variant = FlowVariant.C;
        var statusReference = new StatusReference("status-list-url", 4711);
        var references = new References(List.of(statusReference));
        mockServer.expect(requestTo("%s/pools/%s/new-references".formatted(configuration.getBaseUrl(), configuration.getPoolId(variant))))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", configuration.getApiKey(variant)))
                .andRespond(MockRestResponseCreators.withSuccess(objectMapper.writeValueAsString(references), MediaType.APPLICATION_JSON));

        // When
        var result = statusListAdapter.acquireFreeIndex(variant);

        // Then
        assertThat(result.getIndex()).isEqualTo(statusReference.getIndex());
        assertThat(result.getUri()).isEqualTo(statusReference.getUri());
        mockServer.verify();
    }

    @Test
    void shouldNotGetReference() {
        // Given
        mockServer.expect(requestTo("%s/pools/%s/new-references".formatted(configuration.getBaseUrl(), configuration.getPoolId(FlowVariant.C1))))
                .andRespond(MockRestResponseCreators.withGatewayTimeout());

        // When, Then
        assertThatThrownBy(() -> statusListAdapter.acquireFreeIndex(FlowVariant.C1))
                .isInstanceOf(PidServerException.class)
                .hasMessage("Could not acquire free index from status list service!")
                .hasCauseInstanceOf(RestClientException.class);
    }
}