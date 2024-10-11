/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.openid4vc.vci.service.attestation.AttestationConstants;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidClientException;
import de.bdr.pidi.authorization.core.exception.OIDException;
import de.bdr.pidi.testdata.TestUtils;
import de.bdr.pidi.walletattestation.WalletAttestationService;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ClientAttestationHandlerTest {

    @Mock
    private WalletAttestationService walletAttestationService;
    @InjectMocks
    private ClientAttestationHandler out;

    private HashMap<String, String> params;
    private HttpRequest<?> request;
    private WResponseBuilder responseBuilder;
    private WSessionImpl session;

    private static final String CLIENT_ATTESTATION_JWT = TestUtils.getValidClientAttestationJwt().serialize();
    private static final String CLIENT_ATTESTATION_POP_JWT = TestUtils.getValidClientAttestationPopJwt(FlowVariant.C).serialize();
    private static final String CLIENT_ASSERTION = TestUtils.getValidClientAssertion(FlowVariant.C);

    @BeforeEach
    void setUp() {
        params = new HashMap<>();
        request = RequestUtil.getHttpRequest(params);
        responseBuilder = new WResponseBuilder();
        session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
    }

    public static void initValidClientAttestationParams(Map<String, String> params) {
        assertNotNull(params);

        params.put("client_assertion_type", AttestationConstants.CLIENT_ASSERTION_TYPE);
        params.put("client_assertion", CLIENT_ASSERTION);
    }

    public static void initInvalidClientAttestationMissingType(Map<String, String> params) {
        assertNotNull(params);

        params.put("client_assertion", CLIENT_ASSERTION);
    }

    public static void initInvalidClientAttestationUnknownType(Map<String, String> params) {
        assertNotNull(params);

        params.put("client_assertion_type", "UNKNOWN");
        params.put("client_assertion", CLIENT_ASSERTION);
    }

    public static void initInvalidClientAttestationMissingJwt(Map<String, String> params) {
        assertNotNull(params);

        params.put("client_assertion_type", AttestationConstants.CLIENT_ASSERTION_TYPE);
    }

    public static void initInvalidClientAttestationEmptyType(Map<String, String> params) {
        assertNotNull(params);

        params.put("client_assertion", CLIENT_ASSERTION);
        params.put("client_assertion_type", "");
    }

    public static void initInvalidClientAttestationEmptyJwt(Map<String, String> params) {
        assertNotNull(params);

        params.put("client_assertion", "");
        params.put("client_assertion_type", AttestationConstants.CLIENT_ASSERTION_TYPE);
    }

    public static void initInvalidClientAttestationInvalidJwt(Map<String, String> params) {
        assertNotNull(params);

        params.put("client_assertion", CLIENT_ATTESTATION_JWT + " " + CLIENT_ATTESTATION_POP_JWT);
        params.put("client_assertion_type", AttestationConstants.CLIENT_ASSERTION_TYPE);
    }

    public static void initInvalidClientAttestationBrokenJwt(Map<String, String> params) {
        assertNotNull(params);

        params.put("client_assertion", CLIENT_ATTESTATION_JWT + "~" + "eybbbrrrrroookkkkeeeennnnnnn");
        params.put("client_assertion_type", AttestationConstants.CLIENT_ASSERTION_TYPE);
    }

    @DisplayName("Verify client_assertion valid on auth request")
    @Test
    void test001() {
        initValidClientAttestationParams(params);
        doReturn(true).when(walletAttestationService).isValidWallet(any());

        out.processPushedAuthRequest(request, responseBuilder, session);

        verify(walletAttestationService).isValidWallet(any());
        var clientInstanceKey = session.getParameter(SessionKey.CLIENT_INSTANCE_KEY);
        assertDoesNotThrow(() -> JWK.parse(clientInstanceKey));
    }

    @DisplayName("Verify exception when client_assertion_type is missing on auth request")
    @Test
    void test002() {
        initInvalidClientAttestationMissingType(params);

        assertThatThrownBy(() -> out.processPushedAuthRequest(request, responseBuilder, session))
                .hasMessage("Client assertion type is missing")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidClientException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_client");
    }

    @DisplayName("Verify exception when client_assertion_type is empty on auth request")
    @Test
    void test003() {
        initInvalidClientAttestationEmptyType(params);

        assertThatThrownBy(() -> out.processPushedAuthRequest(request, responseBuilder, session))
                .hasMessage("Client assertion type is empty")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidClientException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_client");
    }

    @DisplayName("Verify exception when client_assertion_type is empty on auth request")
    @Test
    void test004() {
        initInvalidClientAttestationUnknownType(params);

        assertThatThrownBy(() -> out.processPushedAuthRequest(request, responseBuilder, session))
                .hasMessage("Client assertion type is invalid")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidClientException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_client");
    }

    @DisplayName("Verify exception when client_assertion is missing on auth request")
    @Test
    void test005() {
        initInvalidClientAttestationMissingJwt(params);

        assertThatThrownBy(() -> out.processPushedAuthRequest(request, responseBuilder, session))
                .hasMessage("Client assertion is missing")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidClientException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_client");
    }

    @DisplayName("Verify exception when client_assertion is empty on auth request")
    @Test
    void test006() {
        initInvalidClientAttestationEmptyJwt(params);

        assertThatThrownBy(() -> out.processPushedAuthRequest(request, responseBuilder, session))
                .hasMessage("Client assertion is empty")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidClientException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_client");
    }

    @DisplayName("Verify exception when client_assertion is invalid on auth request")
    @Test
    void test007() {
        initInvalidClientAttestationInvalidJwt(params);

        assertThatThrownBy(() -> out.processPushedAuthRequest(request, responseBuilder, session))
                .hasMessage("Client assertion length is invalid")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidClientException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_client");
    }

    @DisplayName("Verify exception when client_assertion could not be parsed on auth request")
    @Test
    void test008() {
        initInvalidClientAttestationBrokenJwt(params);

        assertThatThrownBy(() -> out.processPushedAuthRequest(request, responseBuilder, session))
                .hasMessage("Client assertion could not be parsed")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidClientException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_client");
    }

    @DisplayName("Verify exception when validation throws IllegalArgumentException on auth request")
    @Test
    void test009() {
        initValidClientAttestationParams(params);
        doThrow(new IllegalArgumentException("for the test")).when(walletAttestationService).isValidWallet(any());

        assertThatThrownBy(() -> out.processPushedAuthRequest(request, responseBuilder, session))
                .hasMessage("Client attestation jwt is invalid, for the test")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidClientException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_client");
    }
}
