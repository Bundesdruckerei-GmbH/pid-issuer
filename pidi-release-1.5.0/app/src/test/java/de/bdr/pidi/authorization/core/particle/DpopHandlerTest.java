/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import com.nimbusds.oauth2.sdk.token.DPoPAccessToken;
import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.data.TokenType;
import de.bdr.openid4vc.vci.service.HttpHeaders;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.Nonce;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidGrantException;
import de.bdr.pidi.authorization.core.exception.OIDException;
import de.bdr.pidi.authorization.core.exception.UnauthorizedException;
import de.bdr.pidi.authorization.core.service.NonceService;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;

import java.net.MalformedURLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import static de.bdr.pidi.authorization.core.particle.DpopHandler.DPOP_NONCE_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class DpopHandlerTest {

    public static final HttpMethod METHOD = HttpMethod.POST;
    public static final String URI = "http://localhost";
    public static final String TOKEN_PATH = "/token";
    public static final String CREDENTIAL_PATH = "/credential";

    @Mock
    private NonceService nonceService;

    private DpopHandler handler;

    @BeforeEach
    void setUp() throws MalformedURLException {
        handler = new DpopHandler(nonceService, Duration.ofSeconds(30), Duration.ofSeconds(30), java.net.URI.create("http://base-url").toURL(), TokenType.DPOP.getValue(), false);
    }

    @Test
    @DisplayName("Verify dpop validation and generation on token request is successful")
    void test001() {
        WSession session = new WSessionImpl(FlowVariant.C1, 1L);
        var responseBuilder = new WResponseBuilder();
        var nonce = prepareNonce();
        doReturn(nonce).when(nonceService).fetchDpopNonceFromSession(any());
        Mockito.when(nonceService.generateAndStoreDpopNonce(session)).thenReturn(nonce);
        var request = prepareTokenDpopRequest(nonce);

        assertDoesNotThrow(() -> handler.processTokenRequest(request, responseBuilder, session));
        assertThat(session.getParameter(SessionKey.DPOP_PUBLIC_KEY))
                .isEqualTo(TestUtils.DEVICE_PUBLIC_KEY.toJSONString());
        assertThat(session.getParameter(SessionKey.DPOP_IDENTIFIER))
                .isNotBlank();
    }

    @Test
    @DisplayName("Verify dpop validation on credential request is successful")
    void test002() {
        WSession session = new WSessionImpl(FlowVariant.C1, 1L);
        session.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.DEVICE_PUBLIC_KEY.toJSONString());
        var responseBuilder = new WResponseBuilder();

        var nonce = prepareNonce();
        doReturn(nonce).when(nonceService).fetchDpopNonceFromSession(any());
        var request = prepareCredentialDpopRequest(nonce);

        assertDoesNotThrow(() -> handler.processCredentialRequest((HttpRequest<CredentialRequest>) request, responseBuilder, session));
    }

    @Test
    @DisplayName("Verify exception when dpop header not present on token request")
    void test003() {
        WSession session = new WSessionImpl(FlowVariant.C1, 1L);
        var responseBuilder = new WResponseBuilder();
        var request = RequestUtil.getHttpRequest(Collections.emptyMap());
        doReturn(prepareNonce()).when(nonceService).generateAndStoreDpopNonce(any());

        assertThatThrownBy(() -> handler.processTokenRequest(request, responseBuilder, session))
                .hasMessage("DPoP header not present")
                .asInstanceOf(type(InvalidDpopProofException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_dpop_proof");
    }

    @Test
    @DisplayName("Verify exception when dpop header not present on credential request")
    void test004() {
        WSession session = new WSessionImpl(FlowVariant.C1, 1L);
        session.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.DEVICE_PUBLIC_KEY.toJSONString());
        var responseBuilder = new WResponseBuilder();
        var request = RequestUtil.getHttpRequest(Collections.emptyMap());
        doReturn(prepareNonce()).when(nonceService).generateAndStoreDpopNonce(any());

        assertThatThrownBy(() -> handler.processCredentialRequest((HttpRequest<CredentialRequest>) request, responseBuilder, session))
                .hasMessage("missing dpop proof")
                .asInstanceOf(type(UnauthorizedException.class))
                .extracting(UnauthorizedException::getError).isEqualTo("invalid_dpop_proof");
    }

    @Test
    @DisplayName("Verify exception when dpop invalid on token request")
    void test005() {
        WSession session = new WSessionImpl(FlowVariant.C1, 1L);
        var responseBuilder = new WResponseBuilder();
        var nonce = prepareNonce();
        doReturn(nonce).when(nonceService).generateAndStoreDpopNonce(any());
        var request = prepareTokenDpopRequestWithoutNonce();

        assertThatThrownBy(() -> handler.processTokenRequest(request, responseBuilder, session))
                .hasMessage("nonce value missing")
                .asInstanceOf(type(UseDpopNonceException.class))
                .extracting(OIDException::getErrorCode, e -> e.getHeader().get(DPOP_NONCE_HEADER))
                .containsExactly("use_dpop_nonce", nonce.nonce());
    }

    @Test
    @DisplayName("Verify exception when dpop invalid on credential request")
    void test006() {
        WSession session = new WSessionImpl(FlowVariant.C1, 1L);
        session.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.DEVICE_PUBLIC_KEY.toJSONString());
        var responseBuilder = new WResponseBuilder();
        var nonce = prepareNonce();
        doReturn(nonce).when(nonceService).generateAndStoreDpopNonce(any());
        var request = prepareCredentialDpopRequestWithoutNonce();

        var ueAssert = assertThatThrownBy(() -> handler.processCredentialRequest((HttpRequest<CredentialRequest>) request, responseBuilder, session))
                .hasMessage("nonce value missing")
                .asInstanceOf(type(UnauthorizedException.class));
        ueAssert.extracting(UnauthorizedException::getError)
                .isEqualTo("use_dpop_nonce");
        ueAssert.extracting(Throwable::getCause)
                .asInstanceOf(type(UseDpopNonceException.class))
                .extracting(e -> e.getHeader().get(DPOP_NONCE_HEADER))
                .isEqualTo(nonce.nonce());
    }

    @Test
    @DisplayName("Verify exception when dpop nonce expired on token request")
    void test007() {
        WSession session = new WSessionImpl(FlowVariant.C1, 1L);
        var responseBuilder = new WResponseBuilder();
        var nonce = new Nonce(RandomUtil.randomString(), Duration.ofSeconds(30), Instant.now().minusSeconds(10));
        doReturn(nonce).when(nonceService).fetchDpopNonceFromSession(any());
        doReturn(prepareNonce()).when(nonceService).generateAndStoreDpopNonce(any());
        var request = prepareTokenDpopRequest(nonce);

        assertThatThrownBy(() -> handler.processTokenRequest(request, responseBuilder, session))
                .hasMessage("DPoP nonce is expired")
                .asInstanceOf(type(InvalidDpopProofException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_dpop_proof");
    }

    @Test
    @DisplayName("Verify exception when dpop nonce expired on credential request")
    void test008() {
        WSession session = new WSessionImpl(FlowVariant.C1, 1L);
        session.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.DEVICE_PUBLIC_KEY.toJSONString());
        var responseBuilder = new WResponseBuilder();
        var nonce = new Nonce(RandomUtil.randomString(), Duration.ofSeconds(30), Instant.now().minusSeconds(10));
        doReturn(nonce).when(nonceService).fetchDpopNonceFromSession(any());
        doReturn(prepareNonce()).when(nonceService).generateAndStoreDpopNonce(any());
        var request = prepareCredentialDpopRequest(nonce);

        assertThatThrownBy(() -> handler.processCredentialRequest((HttpRequest<CredentialRequest>) request, responseBuilder, session))
                .hasMessage("DPoP nonce is expired")
                .asInstanceOf(type(UnauthorizedException.class))
                .extracting(UnauthorizedException::getError).isEqualTo("invalid_dpop_proof");
    }

    @Test
    @DisplayName("Verify exception when dpop nonce is invalid on token request")
    void test009() {
        WSession session = new WSessionImpl(FlowVariant.C1, 1L);
        var responseBuilder = new WResponseBuilder();
        doReturn(prepareNonce()).when(nonceService).fetchDpopNonceFromSession(any());
        doReturn(prepareNonce()).when(nonceService).generateAndStoreDpopNonce(any());
        var request = prepareTokenDpopRequest(prepareNonce());

        assertThatThrownBy(() -> handler.processTokenRequest(request, responseBuilder, session))
                .hasMessage("DPoP nonce is invalid")
                .asInstanceOf(type(UseDpopNonceException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("use_dpop_nonce");
    }

    @Test
    @DisplayName("Verify exception when dpop nonce is invalid on credential request")
    void test010() {
        WSession session = new WSessionImpl(FlowVariant.C1, 1L);
        session.putParameter(SessionKey.DPOP_PUBLIC_KEY, TestUtils.DEVICE_PUBLIC_KEY.toJSONString());
        var responseBuilder = new WResponseBuilder();
        doReturn(prepareNonce()).when(nonceService).fetchDpopNonceFromSession(any());
        doReturn(prepareNonce()).when(nonceService).generateAndStoreDpopNonce(any());
        var request = prepareCredentialDpopRequest(prepareNonce());

        assertThatThrownBy(() -> handler.processCredentialRequest((HttpRequest<CredentialRequest>) request, responseBuilder, session))
                .hasMessage("DPoP nonce is invalid")
                .asInstanceOf(type(UnauthorizedException.class))
                .extracting(UnauthorizedException::getError).isEqualTo("use_dpop_nonce");
    }

    @Test
    @DisplayName("Verify exception when jwk is not known on token request")
    @Disabled("PIDI-1855: Temporarily disable Client Attestation over all flows")
    void test011() {
        WSession session = new WSessionImpl(FlowVariant.B1, 1L);
        session.putParameter(SessionKey.CLIENT_INSTANCE_KEY, TestUtils.DIFFERENT_KEY_PAIR.toPublicJWK().toJSONString());
        var responseBuilder = new WResponseBuilder();
        var nonce = prepareNonce();
        doReturn(nonce).when(nonceService).fetchDpopNonceFromSession(any());
        var request = prepareTokenDpopRequest(nonce);

        assertThatThrownBy(() -> handler.processTokenRequest(request, responseBuilder, session))
                .hasMessage("Key mismatch")
                .asInstanceOf(type(InvalidGrantException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_grant");
    }

    @DisplayName("Verify dpop generation on token request is successful")
    @Test
    void test012() {
        WSession session = new WSessionImpl(FlowVariant.C1, 1L);
        var responseBuilder = Mockito.mock(WResponseBuilder.class);
        var nonce = new Nonce("noncense", Duration.ofSeconds(30));
        Mockito.when(nonceService.generateAndStoreDpopNonce(session)).thenReturn(nonce);
        var request = Mockito.mock(HttpRequest.class);

        this.handler.processFinishAuthRequest(request, responseBuilder, session);

        Assertions.assertAll(
                () -> Mockito.verify(responseBuilder).addStringHeader("DPoP-Nonce", nonce.nonce())

        );
    }

    private Nonce prepareNonce() {
        return new Nonce(RandomUtil.randomString(), Duration.ofSeconds(30));
    }

    private HttpRequest<?> prepareTokenDpopRequest(Nonce nonce) {
        var dpopJwt = TestUtils.getDpopProof(METHOD, java.net.URI.create(URI + TOKEN_PATH), nonce.nonce());

        var header = new LinkedMultiValueMap<String, String>();
        header.set("dpop", dpopJwt.serialize());
        header.set("host", "localhost");

        return HttpRequest.Companion.bodyless(METHOD.name(), URI, TOKEN_PATH, new HttpHeaders(header), Collections.emptyMap());
    }

    private HttpRequest<?> prepareCredentialDpopRequest(Nonce nonce) {
        var accessToken = TestUtils.generateAccessToken();

        var dpopJwt = TestUtils.getDpopProof(METHOD, java.net.URI.create(URI + CREDENTIAL_PATH), accessToken, nonce.nonce());

        var header = new LinkedMultiValueMap<String, String>();
        header.set("dpop", dpopJwt.serialize());
        header.set("host", "localhost");
        header.set("authorization", "DPoP " + accessToken);

        return HttpRequest.Companion.bodyless(METHOD.name(), URI, CREDENTIAL_PATH, new HttpHeaders(header), Collections.emptyMap());
    }

    private HttpRequest<?> prepareTokenDpopRequestWithoutNonce() {
        var dpopJwt = TestUtils.getDpopProof(TestUtils.DEVICE_KEY_PAIR, METHOD, java.net.URI.create(URI + TOKEN_PATH), null, null, null, null);
        var header = new LinkedMultiValueMap<String, String>();
        header.set("dpop", dpopJwt.serialize());
        header.set("host", "localhost");

        return HttpRequest.Companion.bodyless(METHOD.name(), URI, TOKEN_PATH, new HttpHeaders(header), Collections.emptyMap());
    }

    private HttpRequest<?> prepareCredentialDpopRequestWithoutNonce() {
        var accessToken = TestUtils.generateAccessToken();

        var dpopJwt = TestUtils.getDpopProof(TestUtils.DEVICE_KEY_PAIR, METHOD, java.net.URI.create(URI + TOKEN_PATH), new DPoPAccessToken(accessToken), null, null, null);
        var header = new LinkedMultiValueMap<String, String>();
        header.set("dpop", dpopJwt.serialize());
        header.set("host", "localhost");
        header.set("authorization", "DPoP " + accessToken);

        return HttpRequest.Companion.bodyless(METHOD.name(), URI, TOKEN_PATH, new HttpHeaders(header), Collections.emptyMap());
    }
}