/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.flows;

import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProof;
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProofType;
import de.bdr.openid4vc.vci.service.HttpHeaders;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.NonceFactory;
import de.bdr.pidi.authorization.core.SessionManager;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.Nonce;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.particle.ClientAttestationHandlerTest;
import de.bdr.pidi.authorization.core.particle.PkceHandlerTest;
import de.bdr.pidi.authorization.core.particle.RequestUtil;
import de.bdr.pidi.authorization.core.service.KeyProofService;
import de.bdr.pidi.authorization.core.service.KeyProofServiceImpl;
import de.bdr.pidi.authorization.core.service.NonceService;
import de.bdr.pidi.authorization.core.service.PidSerializer;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import de.bdr.pidi.authorization.out.identification.IdentificationApi;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.MdocBuilder;
import de.bdr.pidi.authorization.out.issuance.SdJwtBuilder;
import de.bdr.pidi.authorization.out.issuance.SeedPidBuilder;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialRequest;
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialRequest;
import de.bdr.pidi.clientconfiguration.ClientConfigurationService;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.TestConfig;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.bdr.pidi.authorization.ConfigTestData.AUTH_CONFIG;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BFlowControllerTest {

    @Mock
    private SessionManager sessionManager;
    @Mock
    private WSessionImpl session;
    @Mock
    private HttpRequest<?> request;
    @Mock
    private HttpRequest<?> finishAuthorizationRequest;
    @Mock
    private ClientConfigurationService clientConfigurationService;
    @Mock
    private NonceService nonceService;
    @Mock
    private IdentificationApi identificationProvider;
    @Mock
    private SdJwtBuilder<SdJwtVcAuthChannelCredentialRequest> sdJwtBuilder;
    @Mock
    private MdocBuilder<MsoMdocAuthChannelCredentialRequest> mdocBuilder;
    @Mock
    private SeedPidBuilder seedPidBuilder;
    @Spy
    private AuthorizationConfiguration authConfig = AUTH_CONFIG;
    @Spy
    private PidSerializer pidSerializer = new PidSerializer();
    @Spy
    private KeyProofService keyProofService = new KeyProofServiceImpl(AUTH_CONFIG);

    @InjectMocks
    BFlowController out;

    @Test
    void testPar_ok() {
        when(clientConfigurationService.isValidClientId(any(UUID.class))).thenReturn(true);
        var params = new HashMap<String, String>();
        params.put("redirect_uri", "https://example.com/foo");
        params.put("client_id", UUID.randomUUID().toString());
        params.put("response_type", "code");
        params.put("scope", "pid");
        PkceHandlerTest.initValidAuthRequestParams(params);
        ClientAttestationHandlerTest.initValidClientAttestationParams(params);

        when(request.getParameters()).thenReturn(params);
        when(sessionManager.init(FlowVariant.B)).thenReturn(session);
        when(session.isNextAllowedRequest(Requests.PUSHED_AUTHORIZATION_REQUEST)).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> out.processPushedAuthRequest(request));

        verify(sessionManager).init(FlowVariant.B);
        verify(sessionManager).persist(any(WSession.class));
    }

    @Test
    void testAuth_ok() throws MalformedURLException, URISyntaxException {
        var params = new HashMap<String, String>();
        params.put("request_uri", "https://example.com/foo");
        String clientId = UUID.randomUUID().toString();
        params.put("client_id", clientId);
        PkceHandlerTest.initValidAuthRequestParams(params);
        var samlRedirectUrl = "https://saml.request.io/path?SAMLRequest=abcde";

        when(clientConfigurationService.isValidClientId(any(UUID.class))).thenReturn(true);
        when(request.getParameters()).thenReturn(params);
        when(sessionManager.loadByRequestUri(anyString(), eq(FlowVariant.B))).thenReturn(session);
        when(session.isNextAllowedRequest(Requests.AUTHORIZATION_REQUEST)).thenReturn(true);
        when(session.getParameter(SessionKey.CLIENT_ID)).thenReturn(clientId);
        when(session.getCheckedParameterAsInstant(SessionKey.REQUEST_URI_EXP_TIME)).thenReturn(Instant.now().plus(AUTH_CONFIG.getRequestUriLifetime()));
        when(session.getFlowVariant()).thenReturn(FlowVariant.B);
        when(identificationProvider.startIdentificationProcess(any(), anyString(), anyString())).
                thenReturn(new URI(samlRedirectUrl).toURL());

        Assertions.assertDoesNotThrow(() -> out.processAuthRequest(request));
    }

    @Test
    void testFinishAuth_ok() {
        var params = new HashMap<String, String>();
        var issuerState = RandomUtil.randomString();
        var clientId = UUID.randomUUID().toString();
        var dpopNonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getDpopNonceLifetime());

        params.put("issuer_state", issuerState);
        params.put("client_id", clientId);

        when(finishAuthorizationRequest.getParameters()).thenReturn(params);
        when(sessionManager.loadByIssuerState(anyString(), eq(FlowVariant.B))).thenReturn(session);
        when(session.isNextAllowedRequest(Requests.FINISH_AUTHORIZATION_REQUEST)).thenReturn(true);
        when(session.containsParameter(SessionKey.STATE)).thenReturn(true);
        when(session.getParameter(SessionKey.STATE)).thenReturn("StateGivenFromClient");
        when(session.getParameter(SessionKey.ISSUER_STATE)).thenReturn(issuerState);
        when(session.getParameter(SessionKey.REDIRECT_URI)).thenReturn("https://pidi.client/test");
        when(session.getParameter(SessionKey.IDENTIFICATION_RESULT)).thenReturn("Success");
        when(nonceService.generateAndStoreDpopNonce(session)).thenReturn(dpopNonce);

        Assertions.assertDoesNotThrow(() -> out.processFinishAuthRequest(finishAuthorizationRequest));
    }

    @Test
    void testToken_ok() {
        var params = new HashMap<String, String>();
        PkceHandlerTest.initValidTokenRequestParams(params);
        params.put("code", "code value");
        params.put("redirect_uri", "https://pidi.client/test");
        params.put("grant_type", "authorization_code");
        var sessionParams = new HashMap<String, String>();
        PkceHandlerTest.initValidAuthRequestParams(sessionParams);

        var dpopNonce = new Nonce(RandomUtil.randomString(), Duration.ofSeconds(30));
        var headers = new LinkedMultiValueMap<String, String>();
        var dpopProof = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + "/token"), dpopNonce.nonce()).serialize();
        headers.add("dpop", dpopProof);
        headers.add("host", URI.create(TestConfig.pidiBaseUrl()).getAuthority());

        when(session.getParameter(SessionKey.CODE_CHALLENGE)).thenReturn(sessionParams.get("code_challenge"));
        when(session.getCheckedParameterAsInstant(SessionKey.AUTHORIZATION_CODE_EXP_TIME)).thenReturn(Instant.now().plusSeconds(10));
        when(session.getParameter(SessionKey.REDIRECT_URI)).thenReturn("https://pidi.client/test");

        try (MockedStatic<NonceFactory> nonceFactoryMock = mockStatic(NonceFactory.class)) {
            when(request.getHeaders()).thenReturn(new HttpHeaders(headers));
            when(request.getMethod()).thenReturn("POST");
            when(request.getPath()).thenReturn("/token");
            when(request.getParameters()).thenReturn(params);
            when(sessionManager.loadByAuthorizationCode(anyString(), eq(FlowVariant.B))).thenReturn(session);
            when(session.isNextAllowedRequest(Requests.TOKEN_REQUEST)).thenReturn(true);
            nonceFactoryMock.when(() -> NonceFactory.createSecureRandomNonce(AUTH_CONFIG.getAccessTokenLifetime()))
                    .thenReturn(new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getAccessTokenLifetime()));
            when(nonceService.fetchDpopNonceFromSession(any())).thenReturn(dpopNonce);
            when(nonceService.generateAndStoreDpopNonce(any())).thenReturn(new Nonce(RandomUtil.randomString(), Duration.ofSeconds(30)));

            Assertions.assertDoesNotThrow(() -> out.processTokenRequest(request));
        }
    }

    @Test
    void testCredential_sdJwt_ok() throws ParseException {
        var localSession = new WSessionImpl(FlowVariant.B, TestUtils.randomSessionId());
        localSession.setNextExpectedRequest(Requests.CREDENTIAL_REQUEST);
        localSession.putParameter(SessionKey.ACCESS_TOKEN_EXP_TIME, Instant.now().plus(AUTH_CONFIG.getAccessTokenLifetime()));
        PidCredentialData pidCredentialData = PidCredentialData.Companion.getTEST_DATA_SET();
        localSession.putParameter(SessionKey.IDENTIFICATION_DATA, pidSerializer.toString(pidCredentialData));
        HttpRequest<CredentialRequest> credentialRequest = initValidCredentialRequest(localSession);

        var accessToken = TestUtils.generateAccessToken();
        var nonce = new Nonce(RandomUtil.randomString(), Duration.ofSeconds(30));
        var dpopJwt = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + "/b/credential"), accessToken, nonce.nonce());
        localSession.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.DEVICE_PUBLIC_KEY.toJSONString());

        Map<String, List<String>> header = credentialRequest.getHeaders().getHeaders();
        header.put("dpop", List.of(dpopJwt.serialize()));
        header.put("host", List.of(URI.create(TestConfig.pidiBaseUrl()).getAuthority()));
        header.put("authorization", List.of("DPoP " + accessToken));

        ReflectionTestUtils.setField(credentialRequest, "path", "/b/credential");
        ReflectionTestUtils.setField(credentialRequest, "uri", URI.create(TestConfig.pidiBaseUrl()).getAuthority());

        try (MockedStatic<NonceFactory> nonceFactoryMock = mockStatic(NonceFactory.class)) {
            when(sessionManager.loadByAccessToken(eq(AUTH_CONFIG.getAuthorizationScheme()), anyString(), eq(FlowVariant.B))).thenReturn(localSession);
            when(sdJwtBuilder.build(eq(pidCredentialData), eq((SdJwtVcAuthChannelCredentialRequest) credentialRequest.getBody()), anyString())).thenReturn("sdJwt");
            nonceFactoryMock.when(() -> NonceFactory.createSecureRandomNonce(AUTH_CONFIG.getAccessTokenLifetime()))
                    .thenReturn(new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getAccessTokenLifetime()));
            when(nonceService.fetchDpopNonceFromSession(localSession)).thenReturn(nonce);

            Assertions.assertDoesNotThrow(() -> out.processCredentialRequest(credentialRequest));
        }
    }

    public static HttpRequest<CredentialRequest> initValidCredentialRequest(WSession session) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getProofValidity());
        session.putParameter(SessionKey.CLIENT_ID, clientId);
        session.putParameter(SessionKey.C_NONCE, nonce.nonce());
        session.putParameter(SessionKey.C_NONCE_EXP_TIME, nonce.expirationTime());

        var audience = TestUtils.ISSUER_IDENTIFIER_AUDIENCE + "/" + session.getFlowVariant().urlPath;
        var jwt = TestUtils.buildProofJwt(TestUtils.JWT_PROOF_TYPE, clientId, audience, Instant.now(), nonce.nonce());
        var proof = new JwtProof(jwt.serialize(), JwtProofType.INSTANCE);
        var body = TestUtils.createSdJwtAuthChannelCredentialRequest(proof);
        return RequestUtil.getHttpRequest(body);
    }
}
