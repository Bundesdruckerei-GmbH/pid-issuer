/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.flows;

import com.nimbusds.oauth2.sdk.GrantType;
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialRequest;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest;
import de.bdr.openid4vc.common.vci.CredentialRequest;
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
import de.bdr.pidi.authorization.core.exception.SessionNotFoundException;
import de.bdr.pidi.authorization.core.particle.ClientAttestationHandlerTest;
import de.bdr.pidi.authorization.core.particle.KeyProofHandlerTest;
import de.bdr.pidi.authorization.core.particle.PkceHandlerTest;
import de.bdr.pidi.authorization.core.particle.UseDpopNonceException;
import de.bdr.pidi.authorization.core.service.KeyProofService;
import de.bdr.pidi.authorization.core.service.KeyProofServiceImpl;
import de.bdr.pidi.authorization.core.service.NonceService;
import de.bdr.pidi.authorization.core.service.PidSerializer;
import de.bdr.pidi.authorization.core.service.ServiceTestData;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import de.bdr.pidi.authorization.out.identification.IdentificationApi;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.MdocBuilder;
import de.bdr.pidi.authorization.out.issuance.SdJwtBuilder;
import de.bdr.pidi.authorization.out.issuance.SeedPidBuilder;
import de.bdr.pidi.clientconfiguration.ClientConfigurationService;
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
import static de.bdr.pidi.testdata.ValidTestData.CODE_CHALLANGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class C1FlowControllerTest {

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
    private SdJwtBuilder<SdJwtVcCredentialRequest> sdJwtBuilder;
    @Mock
    private MdocBuilder<MsoMdocCredentialRequest> mdocBuilder;
    @Mock
    private SeedPidBuilder seedPidBuilder;
    @Spy
    private PidSerializer pidSerializer = new PidSerializer();
    @Spy
    private KeyProofService keyProofService = new KeyProofServiceImpl(AUTH_CONFIG);
    @Spy
    private AuthorizationConfiguration authConfig = AUTH_CONFIG;
    @InjectMocks
    private C1FlowController out;

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
        when(sessionManager.init(FlowVariant.C1)).thenReturn(session);
        when(session.isNextAllowedRequest(Requests.PUSHED_AUTHORIZATION_REQUEST)).thenReturn(true);

        out.processPushedAuthRequest(request);

        verify(sessionManager).init(FlowVariant.C1);
        verify(sessionManager).persist(any(WSession.class));
    }

    @Test
    void testAuth_ok() throws MalformedURLException, URISyntaxException {
        var params = new HashMap<String, String>();
        String clientId = UUID.randomUUID().toString();
        params.put("client_id", clientId);
        params.put("request_uri", "https://example.com/foo");
        PkceHandlerTest.initValidAuthRequestParams(params);
        var samlRedirectUrl = "https://saml.request.io/path?SAMLRequest=abcde";
        when(clientConfigurationService.isValidClientId(any(UUID.class))).thenReturn(true);
        when(request.getParameters()).thenReturn(params);
        when(sessionManager.loadByRequestUri(anyString(), eq(FlowVariant.C1))).thenReturn(session);
        when(session.isNextAllowedRequest(Requests.AUTHORIZATION_REQUEST)).thenReturn(true);
        when(session.getParameter(SessionKey.CLIENT_ID)).thenReturn(clientId);
        when(session.getCheckedParameterAsInstant(SessionKey.REQUEST_URI_EXP_TIME)).thenReturn(Instant.now().plus(AUTH_CONFIG.getRequestUriLifetime()));
        when(session.getFlowVariant()).thenReturn(FlowVariant.C1);
        when(identificationProvider.startIdentificationProcess(any(), anyString(), anyString())).
                thenReturn(new URI(samlRedirectUrl).toURL());

        out.processAuthRequest(request);

        verify(sessionManager).loadByRequestUri(anyString(), eq(FlowVariant.C1));
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
        when(sessionManager.loadByIssuerState(anyString(), eq(FlowVariant.C1))).thenReturn(session);
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
        params.put("code", "abcdef");
        params.put("redirect_uri", "https://pidi.client/test");
        params.put("grant_type", GrantType.AUTHORIZATION_CODE.getValue());
        PkceHandlerTest.initValidTokenRequestParams(params);

        var dpopNonce = new Nonce(RandomUtil.randomString(), Duration.ofSeconds(30));
        var headers = new LinkedMultiValueMap<String, String>();
        var dpopProof = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + "/token"), dpopNonce.nonce()).serialize();
        headers.add("dpop", dpopProof);
        headers.add("host", URI.create(TestConfig.pidiBaseUrl()).getAuthority());

        var pidString = ServiceTestData.createPidString();

        try (MockedStatic<NonceFactory> nonceFactoryMock = mockStatic(NonceFactory.class)) {
            when(request.getParameters()).thenReturn(params);
            when(request.getHeaders()).thenReturn(new HttpHeaders(headers));
            when(request.getMethod()).thenReturn("POST");
            when(request.getPath()).thenReturn("/token");
            when(sessionManager.loadByAuthorizationCode(anyString(), eq(FlowVariant.C1))).thenReturn(session);
            when(session.isNextAllowedRequest(Requests.TOKEN_REQUEST)).thenReturn(true);
            when(session.getParameter(SessionKey.CODE_CHALLENGE)).thenReturn(CODE_CHALLANGE);
            when(session.getParameter(SessionKey.REDIRECT_URI)).thenReturn("https://pidi.client/test");
            when(session.getCheckedParameterAsInstant(SessionKey.AUTHORIZATION_CODE_EXP_TIME)).thenReturn(Instant.now().plusSeconds(10));
            when(session.getCheckedParameterAsJwk(SessionKey.DPOP_PUBLIC_KEY)).thenReturn(TestUtils.DEVICE_PUBLIC_KEY);
            when(session.getCheckedParameter(SessionKey.IDENTIFICATION_DATA)).thenReturn(pidString);
            nonceFactoryMock.when(() -> NonceFactory.createSecureRandomNonce(AUTH_CONFIG.getAccessTokenLifetime()))
                    .thenReturn(new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getAccessTokenLifetime()));
            when(nonceService.fetchDpopNonceFromSession(any())).thenReturn(dpopNonce);
            when(nonceService.generateAndStoreDpopNonce(any())).thenReturn(new Nonce(RandomUtil.randomString(), Duration.ofSeconds(30)));

            Assertions.assertDoesNotThrow(() -> out.processTokenRequest(request));
        }
    }

    @Test
    void testRefreshToken_first_time_not_ok() {
        String clientId = UUID.randomUUID().toString();
        var params = new HashMap<String, String>();
        params.put("client_id", clientId);
        params.put("refresh_token", "abcdef");
        params.put("grant_type", GrantType.REFRESH_TOKEN.getValue());
        ClientAttestationHandlerTest.initValidClientAttestationParams(params);

        var dpopNonce = new Nonce(RandomUtil.randomString(), Duration.ofSeconds(30));
        var dpopProof = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + "/c1/token"), dpopNonce.nonce()).serialize();
        var headers = new LinkedMultiValueMap<String, String>();
        headers.add("dpop", dpopProof);
        headers.add("host", URI.create(TestConfig.pidiBaseUrl()).getAuthority());

        when(clientConfigurationService.isValidClientId(any(UUID.class))).thenReturn(true);
        when(request.getParameters()).thenReturn(params);
        when(request.getHeaders()).thenReturn(new HttpHeaders(headers));
        when(request.getMethod()).thenReturn("POST");
        when(request.getPath()).thenReturn("/c1/token");
        when(sessionManager.initRefresh(eq(FlowVariant.C1), anyString())).thenReturn(session);
        when(sessionManager.loadByRefreshToken(anyString())).thenThrow(SessionNotFoundException.class);
        when(nonceService.fetchDpopNonceFromSession(any())).thenThrow(SessionNotFoundException.class);
        when(nonceService.generateAndStoreDpopNonce(any())).thenReturn(new Nonce(RandomUtil.randomString(), Duration.ofSeconds(30)));

        Assertions.assertThrows(UseDpopNonceException.class, () -> out.processRefreshTokenRequest(request));
    }

    @Test
    void testRefreshToken_second_time_ok() {
        String clientId = UUID.randomUUID().toString();
        var params = new HashMap<String, String>();
        params.put("client_id", clientId);
        params.put("refresh_token", "abcdef");
        params.put("grant_type", GrantType.REFRESH_TOKEN.getValue());
        ClientAttestationHandlerTest.initValidClientAttestationParams(params);

        var jwk = TestUtils.DEVICE_PUBLIC_KEY;
        var seedData = new SeedPidBuilder.SeedData(null, jwk, AUTH_CONFIG.getCredentialIssuerIdentifier(FlowVariant.C1), null, null);
        var dpopNonce = new Nonce(RandomUtil.randomString(), Duration.ofSeconds(30));
        var dpopProof = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + "/c1/token"), dpopNonce.nonce()).serialize();
        var headers = new LinkedMultiValueMap<String, String>();
        headers.add("dpop", dpopProof);
        headers.add("host", URI.create(TestConfig.pidiBaseUrl()).getAuthority());

        try (MockedStatic<NonceFactory> nonceFactoryMock = mockStatic(NonceFactory.class)) {
            when(clientConfigurationService.isValidClientId(any(UUID.class))).thenReturn(true);
            when(request.getParameters()).thenReturn(params);
            when(request.getHeaders()).thenReturn(new HttpHeaders(headers));
            when(request.getMethod()).thenReturn("POST");
            when(request.getPath()).thenReturn("/c1/token");
            when(sessionManager.loadByRefreshToken(anyString())).thenReturn(session);
            nonceFactoryMock.when(() -> NonceFactory.createSecureRandomNonce(AUTH_CONFIG.getAccessTokenLifetime()))
                    .thenReturn(new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getAccessTokenLifetime()));
            when(nonceService.fetchDpopNonceFromSession(any())).thenReturn(dpopNonce);
            when(nonceService.generateAndStoreDpopNonce(any())).thenReturn(new Nonce(RandomUtil.randomString(), Duration.ofSeconds(30)));
            when(session.getCheckedParameterAsJwk(SessionKey.DPOP_PUBLIC_KEY)).thenReturn(jwk);
            when(seedPidBuilder.extractVerifiedEncSeed(anyString(), eq(AUTH_CONFIG.getCredentialIssuerIdentifier(FlowVariant.C1)))).thenReturn(seedData);

            Assertions.assertDoesNotThrow(() -> out.processRefreshTokenRequest(request));
        }
    }

    @Test
    void testCredential_sdJwt_ok() throws ParseException {
        var localSession = new WSessionImpl(FlowVariant.C1, TestUtils.randomSessionId());
        localSession.setNextExpectedRequest(Requests.CREDENTIAL_REQUEST);
        localSession.putParameter(SessionKey.ACCESS_TOKEN_EXP_TIME, Instant.now().plus(AUTH_CONFIG.getAccessTokenLifetime()));
        PidCredentialData pidCredentialData = PidCredentialData.Companion.getTEST_DATA_SET();
        localSession.putParameter(SessionKey.IDENTIFICATION_DATA, pidSerializer.toString(pidCredentialData));
        localSession.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.CLIENT_PUBLIC_KEY.toJSONString());
        HttpRequest<CredentialRequest> credentialRequest = KeyProofHandlerTest.initValidKeyProofRequest(localSession);

        var accessToken = TestUtils.generateAccessToken();
        var nonce = new Nonce(RandomUtil.randomString(), Duration.ofSeconds(30));
        var dpopJwt = TestUtils.getDpopProof(HttpMethod.POST, URI.create(TestConfig.pidiBaseUrl() + "/c1/credential"), accessToken, nonce.nonce());
        localSession.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.DEVICE_PUBLIC_KEY.toJSONString());

        Map<String, List<String>> header = credentialRequest.getHeaders().getHeaders();
        header.put("dpop", List.of(dpopJwt.serialize()));
        header.put("host", List.of(URI.create(TestConfig.pidiBaseUrl()).getAuthority()));
        header.put("authorization", List.of("DPoP " + accessToken));

        ReflectionTestUtils.setField(credentialRequest, "path", "/c1/credential");
        ReflectionTestUtils.setField(credentialRequest, "uri", URI.create(TestConfig.pidiBaseUrl()).getAuthority());

        try (MockedStatic<NonceFactory> nonceFactoryMock = mockStatic(NonceFactory.class)) {
            when(sessionManager.loadByAccessToken(eq(AUTH_CONFIG.getAuthorizationScheme()), anyString(), eq(FlowVariant.C1))).thenReturn(localSession);
            when(sdJwtBuilder.build(eq(pidCredentialData), eq((SdJwtVcCredentialRequest) credentialRequest.getBody()), anyString())).thenReturn("sdJwt");
            nonceFactoryMock.when(() -> NonceFactory.createSecureRandomNonce(AUTH_CONFIG.getAccessTokenLifetime()))
                    .thenReturn(new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getAccessTokenLifetime()));
            when(nonceService.fetchDpopNonceFromSession(localSession)).thenReturn(nonce);

            Assertions.assertDoesNotThrow(() -> out.processCredentialRequest(credentialRequest));
        }
    }
}
