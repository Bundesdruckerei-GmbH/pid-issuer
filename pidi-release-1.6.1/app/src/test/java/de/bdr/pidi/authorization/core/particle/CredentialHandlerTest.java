/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import com.fasterxml.jackson.databind.JsonNode;
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialRequest;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest;
import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProof;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.service.PidSerializer;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.MdocBuilder;
import de.bdr.pidi.authorization.out.issuance.SdJwtBuilder;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialRequest;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialHandlerTest {
    @Mock
    private SdJwtBuilder mockedSdJwtBuilder;

    @Mock
    private MdocBuilder<MsoMdocCredentialRequest> mockedMdocBuilder;

    @Mock
    private MdocBuilder<MsoMdocAuthChannelCredentialRequest> mockedMdocAuthChannelBuilder;

    private final PidSerializer pidSerializer = new PidSerializer();
    private final List<Class<? extends CredentialRequest>> requestsUsingProof = List.of(SdJwtVcCredentialRequest.class, MsoMdocCredentialRequest.class);

    private CredentialHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CredentialHandler(mockedSdJwtBuilder, mockedMdocBuilder, pidSerializer, requestsUsingProof);
    }

    @Test
    void shouldProcessSingleSdJwt() throws ParseException {
        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.putParameter(SessionKey.ACCESS_TOKEN_EXP_TIME, Instant.now().plusSeconds(10));
        var pidCredentialData = PidCredentialData.Companion.getTEST_DATA_SET();
        session.putParameter(SessionKey.IDENTIFICATION_DATA, pidSerializer.toString(pidCredentialData));
        session.putParameter(SessionKey.VERIFIED_CREDENTIAL_KEY, "credential");
        var responseBuilder = new WResponseBuilder();
        var localRequest = RequestUtil.getHttpRequest(TestUtils.createSdJwtCredentialRequest((JwtProof) null));

        when(mockedSdJwtBuilder.build(pidCredentialData, (SdJwtVcCredentialRequest) localRequest.getBody(), "credential")).thenReturn("sdJwt");

        handler.processCredentialRequest(localRequest, responseBuilder, session);

        var body = responseBuilder.buildJSONResponseEntity().getBody();
        assertThat(body).isNotNull();
        assertThat(body.findValue("credential").asText())
                .isEqualTo("sdJwt");
        assertThat(body.findValue("credentials")).isNull();
    }

    @Test
    void shouldProcessMdoc() throws ParseException {
        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.putParameter(SessionKey.ACCESS_TOKEN_EXP_TIME, Instant.now().plusSeconds(10));
        var pidCredentialData = PidCredentialData.Companion.getTEST_DATA_SET();
        session.putParameter(SessionKey.IDENTIFICATION_DATA, pidSerializer.toString(pidCredentialData));
        session.putParameter(SessionKey.VERIFIED_CREDENTIAL_KEY, "credential");
        var responseBuilder = new WResponseBuilder();
        var localRequest = RequestUtil.getHttpRequest(TestUtils.createMdocCredentialRequest());

        when(mockedMdocBuilder.build(pidCredentialData, (MsoMdocCredentialRequest) localRequest.getBody(), "credential", FlowVariant.C)).thenReturn("mdoc");

        handler.processCredentialRequest(localRequest, responseBuilder, session);

        var body = responseBuilder.buildJSONResponseEntity().getBody();
        assertThat(body).isNotNull();
        assertThat(body.findValue("credential").asText())
                .isEqualTo("mdoc");
        assertThat(body.findValue("credentials")).isNull();
    }

    @Test
    void shouldBatchProcessSdJwt() throws ParseException {
        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.putParameter(SessionKey.ACCESS_TOKEN_EXP_TIME, Instant.now().plusSeconds(10));
        var pidCredentialData = PidCredentialData.Companion.getTEST_DATA_SET();
        session.putParameter(SessionKey.IDENTIFICATION_DATA, pidSerializer.toString(pidCredentialData));
        session.putParameters(SessionKey.VERIFIED_CREDENTIAL_KEYS, List.of("credentialKey1", "credentialKey2"));
        var responseBuilder = new WResponseBuilder();
        var localRequest = RequestUtil.getHttpRequest(TestUtils.createSdJwtCredentialRequest());

        when(mockedSdJwtBuilder.build(eq(pidCredentialData), eq((SdJwtVcCredentialRequest) localRequest.getBody()), anyString())).thenReturn("sdJwt1", "sdJwt2");

        handler.processCredentialRequest(localRequest, responseBuilder, session);

        var body = responseBuilder.buildJSONResponseEntity().getBody();
        assertThat(body).isNotNull();
        assertThat(body.findValue("credentials"))
                .hasSize(2)
                .map(JsonNode::asText)
                .containsExactly("sdJwt1", "sdJwt2");
        assertThat(body.findValue("credential")).isNull();
    }

    @Test
    void shouldBatchProcessMdoc() throws ParseException {
        WSession session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.putParameter(SessionKey.ACCESS_TOKEN_EXP_TIME, Instant.now().plusSeconds(10));
        var pidCredentialData = PidCredentialData.Companion.getTEST_DATA_SET();
        session.putParameter(SessionKey.IDENTIFICATION_DATA, pidSerializer.toString(pidCredentialData));
        session.putParameters(SessionKey.VERIFIED_CREDENTIAL_KEYS, List.of("credentialKey1", "credentialKey2"));
        var responseBuilder = new WResponseBuilder();
        var localRequest = RequestUtil.getHttpRequest(TestUtils.createMdocCredentialRequest());

        when(mockedMdocBuilder.build(eq(pidCredentialData), eq((MsoMdocCredentialRequest) localRequest.getBody()), anyString(), eq(FlowVariant.C))).thenReturn("mdoc1", "mdoc2");

        handler.processCredentialRequest(localRequest, responseBuilder, session);

        var body = responseBuilder.buildJSONResponseEntity().getBody();
        assertThat(body).isNotNull();
        assertThat(body.findValue("credentials"))
                .hasSize(2)
                .map(JsonNode::asText)
                .containsExactly("mdoc1", "mdoc2");
        assertThat(body.findValue("credential")).isNull();
    }

    @Test
    void shouldProcessMdocAuthChannel() throws ParseException {
        handler = new CredentialHandler(mockedSdJwtBuilder, mockedMdocAuthChannelBuilder, pidSerializer, requestsUsingProof);
        WSession session = new WSessionImpl(FlowVariant.B, TestUtils.randomSessionId());
        session.putParameter(SessionKey.ACCESS_TOKEN_EXP_TIME, Instant.now().plusSeconds(10));
        var pidCredentialData = PidCredentialData.Companion.getTEST_DATA_SET();
        session.putParameter(SessionKey.IDENTIFICATION_DATA, pidSerializer.toString(pidCredentialData));
        var responseBuilder = new WResponseBuilder();
        var localRequest = RequestUtil.getHttpRequest(TestUtils.createMdocAuthChannelCredentialRequest());

        when(mockedMdocAuthChannelBuilder.build(pidCredentialData, (MsoMdocAuthChannelCredentialRequest) localRequest.getBody(), null, FlowVariant.B)).thenReturn("mdoc");

        handler.processCredentialRequest(localRequest, responseBuilder, session);

        var body = responseBuilder.buildJSONResponseEntity().getBody();
        assertThat(body).isNotNull();
        assertThat(body.findValue("credential").asText())
                .isEqualTo("mdoc");
        assertThat(body.findValue("credentials")).isNull();
    }
}
