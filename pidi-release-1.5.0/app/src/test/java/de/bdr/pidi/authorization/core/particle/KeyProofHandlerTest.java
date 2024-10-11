/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import com.nimbusds.jose.JOSEObjectType;
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialRequest;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialFormat;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest;
import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.common.vci.proofs.Proof;
import de.bdr.openid4vc.common.vci.proofs.ProofType;
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProof;
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProofType;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.Nonce;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidProofException;
import de.bdr.pidi.authorization.core.exception.OIDException;
import de.bdr.pidi.authorization.core.service.KeyProofServiceImpl;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import de.bdr.pidi.base.PidDataConst;
import de.bdr.pidi.base.PidServerException;
import de.bdr.pidi.testdata.ClientIds;
import de.bdr.pidi.testdata.TestUtils;
import kotlin.reflect.KClass;
import kotlinx.serialization.KSerializer;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static de.bdr.pidi.authorization.ConfigTestData.AUTH_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class KeyProofHandlerTest {
    private static final ProofType VALID_PROOF_TYPE = JwtProofType.INSTANCE;
    private static final JOSEObjectType VALID_JWT_TYPE = TestUtils.JWT_PROOF_TYPE;
    private static final JOSEObjectType INVALID_JWT_TYPE = new JOSEObjectType("invalid");
    private static final String VALID_JWT_AUDIENCE = TestUtils.ISSUER_IDENTIFIER_AUDIENCE;
    private final List<Class<? extends CredentialRequest>> requestsUsingProof = List.of(SdJwtVcCredentialRequest.class, MsoMdocCredentialRequest.class);

    private final KeyProofHandler handler = new KeyProofHandler(new KeyProofServiceImpl(AUTH_CONFIG), requestsUsingProof, true);

    private WResponseBuilder responseBuilder;
    private WSessionImpl session;

    public static HttpRequest<CredentialRequest> initKeyProofRequest(WSession session, String clientId, Nonce nonce, ProofType proofType, JOSEObjectType jwtType, String jwtIssuer, String jwtAudience, Instant jwtIssueTime, String jwtNonce) {
        session.putParameter(SessionKey.CLIENT_ID, clientId);
        session.putParameter(SessionKey.C_NONCE, nonce.nonce());
        session.putParameter(SessionKey.C_NONCE_EXP_TIME, nonce.expirationTime());

        var audience = jwtAudience + "/" + session.getFlowVariant().urlPath;
        var jwt = TestUtils.buildProofJwt(jwtType, jwtIssuer, audience, jwtIssueTime, jwtNonce);
        var proof = new JwtProof(jwt.serialize(), proofType);
        var body = TestUtils.createSdJwtCredentialRequest(proof);
        return RequestUtil.getHttpRequest(body);
    }

    public static HttpRequest<CredentialRequest> initEmptyKeyProofRequest(WSession session) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getProofValidity(), null);

        session.putParameter(SessionKey.CLIENT_ID, clientId);
        session.putParameter(SessionKey.C_NONCE, nonce.nonce());
        session.putParameter(SessionKey.C_NONCE_EXP_TIME, nonce.expirationTime());

        var body = TestUtils.createSdJwtCredentialRequest((JwtProof) null);
        return RequestUtil.getHttpRequest(body);
    }

    public static HttpRequest<CredentialRequest> initKeyProofRequest(WSession session, String clientId, Nonce nonce, ProofType proofType, JOSEObjectType jwtType, String jwtIssuer, String jwtAudience, Instant jwtIssueTime, String jwtNonce, int nrProofs) {
        session.putParameter(SessionKey.CLIENT_ID, clientId);
        session.putParameter(SessionKey.C_NONCE, nonce.nonce());
        session.putParameter(SessionKey.C_NONCE_EXP_TIME, nonce.expirationTime());

        var audience = jwtAudience + "/" + session.getFlowVariant().urlPath;
        var jwt = TestUtils.buildProofJwt(jwtType, jwtIssuer, audience, jwtIssueTime, jwtNonce);
        var proof = new JwtProof(jwt.serialize(), proofType);
        var proofs = Collections.nCopies(nrProofs, (Proof)proof);
        var body = TestUtils.createSdJwtCredentialRequest(proofs);
        return RequestUtil.getHttpRequest(body);
    }

    public static HttpRequest<CredentialRequest> initValidKeyProofRequest(WSession session) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getProofValidity());
        return initKeyProofRequest(session, clientId, nonce, VALID_PROOF_TYPE, VALID_JWT_TYPE, clientId, VALID_JWT_AUDIENCE, Instant.now(), nonce.nonce());
    }

    public static HttpRequest<CredentialRequest> initValidKeyProofRequest(WSession session, int nrProofs) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getProofValidity());
        return initKeyProofRequest(session, clientId, nonce, VALID_PROOF_TYPE, VALID_JWT_TYPE, clientId, VALID_JWT_AUDIENCE, Instant.now(), nonce.nonce(), nrProofs);
    }

    public static HttpRequest<CredentialRequest> initInvalidKeyProofRequestInvalidProofType(WSession session) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getProofValidity());
        return initKeyProofRequest(session, clientId, nonce, new InvalidProofType(), VALID_JWT_TYPE, clientId, VALID_JWT_AUDIENCE, Instant.now(), nonce.nonce());
    }

    public static HttpRequest<CredentialRequest> initInvalidKeyProofRequestInvalidJwtType(WSession session) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getProofValidity());
        return initKeyProofRequest(session, clientId, nonce, VALID_PROOF_TYPE, INVALID_JWT_TYPE, clientId, VALID_JWT_AUDIENCE, Instant.now(), nonce.nonce());
    }

    public static HttpRequest<CredentialRequest> initInvalidKeyProofRequestInvalidIssuer(WSession session) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getProofValidity());
        return initKeyProofRequest(session, clientId, nonce, VALID_PROOF_TYPE, VALID_JWT_TYPE, "different", VALID_JWT_AUDIENCE, Instant.now(), nonce.nonce());
    }

    public static HttpRequest<CredentialRequest> initInvalidKeyProofRequestInvalidAudience(WSession session) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getProofValidity());
        return initKeyProofRequest(session, clientId, nonce, VALID_PROOF_TYPE, VALID_JWT_TYPE, clientId, "invalid", Instant.now(), nonce.nonce());
    }

    public static HttpRequest<CredentialRequest> initInvalidKeyProofRequestIssuanceInFuture(WSession session) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getProofValidity());
        return initKeyProofRequest(session, clientId, nonce, VALID_PROOF_TYPE, VALID_JWT_TYPE, clientId, VALID_JWT_AUDIENCE, Instant.now().plus(AUTH_CONFIG.getProofTimeTolerance()).plusSeconds(10), nonce.nonce());
    }

    public static HttpRequest<CredentialRequest> initInvalidKeyProofRequestIssuanceTooOld(WSession session) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getProofValidity());
        return initKeyProofRequest(session, clientId, nonce, VALID_PROOF_TYPE, VALID_JWT_TYPE, clientId, VALID_JWT_AUDIENCE, Instant.now().minus(AUTH_CONFIG.getProofTimeTolerance()).minus(AUTH_CONFIG.getProofValidity()).minusSeconds(10), nonce.nonce());
    }

    public static HttpRequest<CredentialRequest> initInvalidKeyProofRequestExpiredNonce(WSession session) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getProofValidity(), Instant.now().minus(AUTH_CONFIG.getProofTimeTolerance()).minusSeconds(10));
        return initKeyProofRequest(session, clientId, nonce, VALID_PROOF_TYPE, VALID_JWT_TYPE, clientId, VALID_JWT_AUDIENCE, Instant.now(), nonce.nonce());
    }

    public static HttpRequest<CredentialRequest> initInvalidKeyProofRequestInvalidNonce(WSession session) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getProofValidity());
        return initKeyProofRequest(session, clientId, nonce, VALID_PROOF_TYPE, VALID_JWT_TYPE, clientId, VALID_JWT_AUDIENCE, Instant.now(), "nonce");
    }

    public static HttpRequest<CredentialRequest> initInvalidKeyProofRequestNoProof() {
        var body = new SdJwtVcCredentialRequest(SdJwtVcCredentialFormat.INSTANCE, null, Collections.emptyList(), null, PidDataConst.SD_JWT_VCTYPE);
        return RequestUtil.getHttpRequest(body);
    }

    public static HttpRequest<CredentialRequest> initInvalidKeyProofRequestJwtNotParsable() {
        var proof = new JwtProof("jwt", JwtProofType.INSTANCE);
        var body = new SdJwtVcCredentialRequest(SdJwtVcCredentialFormat.INSTANCE, proof, Collections.emptyList(), null, PidDataConst.SD_JWT_VCTYPE);
        return RequestUtil.getHttpRequest(body);
    }

    public static HttpRequest<CredentialRequest> initKeyProofRequestInvalidSessionNoClientId(WSession session) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getProofValidity());
        return initKeyProofRequest(session, null, nonce, VALID_PROOF_TYPE, VALID_JWT_TYPE, clientId, VALID_JWT_AUDIENCE, Instant.now(), nonce.nonce());
    }

    public static HttpRequest<CredentialRequest> initKeyProofRequestInvalidSessionNoNonce(WSession session) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(null, AUTH_CONFIG.getProofValidity());
        return initKeyProofRequest(session, clientId, nonce, VALID_PROOF_TYPE, VALID_JWT_TYPE, clientId, VALID_JWT_AUDIENCE, Instant.now(), nonce.nonce());
    }

    public static HttpRequest<CredentialRequest> initKeyProofRequestInvalidSessionNoNonceExpirationTime(WSession session) {
        var clientId = ClientIds.validClientId().toString();
        var nonce = new Nonce(RandomUtil.randomString(), AUTH_CONFIG.getProofValidity(), null);
        return initKeyProofRequest(session, clientId, nonce, VALID_PROOF_TYPE, VALID_JWT_TYPE, clientId, VALID_JWT_AUDIENCE, Instant.now(), nonce.nonce());
    }

    @BeforeEach
    void setUp() {
        responseBuilder = new WResponseBuilder();
        session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
    }

    @DisplayName("Verify proof valid on credential request")
    @Test
    void test001() {
        var request = initValidKeyProofRequest(session);

        handler.processCredentialRequest(request, responseBuilder, session);

        var expectedJwk = TestUtils.RELYING_PARTY_PUBLIC_KEY.toJSONString();
        assertThat(session.getCheckedParameter(SessionKey.VERIFIED_CREDENTIAL_KEY)).isEqualTo(expectedJwk);
    }

    @DisplayName("Verify exception when proof_type is invalid on credential request")
    @Test
    @Disabled("We can't create invalid key proof requests here anymore, test needs to move to e2e tests")
    void test002() {
        var request = initInvalidKeyProofRequestInvalidProofType(session);

        assertThatThrownBy(() -> handler.processCredentialRequest(request, responseBuilder, session))
                .hasMessage("Proof type invalid")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidProofException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_proof");

    }

    @DisplayName("Verify exception when jwt type is invalid on credential request")
    @Test
    void test003() {
        var request = initInvalidKeyProofRequestInvalidJwtType(session);

        assertThatThrownBy(() -> handler.processCredentialRequest(request, responseBuilder, session))
                .hasMessage("Proof JWT type mismatch, expected to be openid4vci-proof+jwt")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidProofException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_proof");

    }

    @DisplayName("Verify exception when issuer is invalid on credential request")
    @Test
    void test004() {
        var request = initInvalidKeyProofRequestInvalidIssuer(session);

        assertThatThrownBy(() -> handler.processCredentialRequest(request, responseBuilder, session))
                .hasMessage("Proof JWT issuer invalid")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidProofException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_proof");

    }

    @DisplayName("Verify exception when audience is invalid on credential request")
    @Test
    void test005() {
        var request = initInvalidKeyProofRequestInvalidAudience(session);

        assertThatThrownBy(() -> handler.processCredentialRequest(request, responseBuilder, session))
                .hasMessage("Proof JWT audience invalid")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidProofException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_proof");

    }

    @DisplayName("Verify exception when issuance is in the future on credential request")
    @Test
    void test006() {
        var request = initInvalidKeyProofRequestIssuanceInFuture(session);

        assertThatThrownBy(() -> handler.processCredentialRequest(request, responseBuilder, session))
                .hasMessage("Proof JWT is issued in the future")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidProofException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_proof");

    }

    @DisplayName("Verify exception when issuance is too old on credential request")
    @Test
    void test007() {
        var request = initInvalidKeyProofRequestIssuanceTooOld(session);

        assertThatThrownBy(() -> handler.processCredentialRequest(request, responseBuilder, session))
                .hasMessage("Proof JWT issuance is too old")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidProofException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_proof");

    }

    @DisplayName("Verify exception when nonce is expired on credential request")
    @Test
    void test008() {
        var request = initInvalidKeyProofRequestExpiredNonce(session);

        assertThatThrownBy(() -> handler.processCredentialRequest(request, responseBuilder, session))
                .hasMessage("Proof JWT credential nonce expired")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidProofException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_proof");

    }

    @DisplayName("Verify exception when nonce is invalid on credential request")
    @Test
    void test009() {
        var request = initInvalidKeyProofRequestInvalidNonce(session);

        assertThatThrownBy(() -> handler.processCredentialRequest(request, responseBuilder, session))
                .hasMessage("Proof JWT credential nonce invalid")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidProofException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_proof");

    }

    @DisplayName("Verify exception when no proof provided on credential request")
    @Test
    void test010() {
        var request = initInvalidKeyProofRequestNoProof();

        assertThatThrownBy(() -> handler.processCredentialRequest(request, responseBuilder, session))
                .hasMessage("Proof is missing")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidProofException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_proof");

    }

    @DisplayName("Verify exception when jwt not parsable on credential request")
    @Test
    @Disabled("We can't create invalid key proof requests here anymore, test needs to move to e2e tests")
    void test011() {
        var request = initInvalidKeyProofRequestJwtNotParsable();

        assertThatThrownBy(() -> handler.processCredentialRequest(request, responseBuilder, session))
                .hasMessage("Proof JWT could not be parsed")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidProofException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_proof");

    }

    @DisplayName("Verify exception when session is missing client id")
    @Test
    void test012() {
        var request = initKeyProofRequestInvalidSessionNoClientId(session);

        assertThatThrownBy(() -> handler.processCredentialRequest(request, responseBuilder, session))
                .hasMessage("clientId not found")
                .isInstanceOf(PidServerException.class);
    }

    @DisplayName("Verify exception when session is missing cNonce")
    @Test
    void test013() {
        var request = initKeyProofRequestInvalidSessionNoNonce(session);

        assertThatThrownBy(() -> handler.processCredentialRequest(request, responseBuilder, session))
                .hasMessage("cNonce not found")
                .isInstanceOf(PidServerException.class);
    }

    @DisplayName("Verify exception when session is missing cNonce expiration time")
    @Test
    void test014() {
        var request = initKeyProofRequestInvalidSessionNoNonceExpirationTime(session);

        assertThatThrownBy(() -> handler.processCredentialRequest(request, responseBuilder, session))
                .hasMessage("cNonceExpirationTime not found")
                .isInstanceOf(PidServerException.class);
    }

    @DisplayName("Verify exception when proof is missing and proofs is empty")
    @Test
    void test015() {
        var request = initEmptyKeyProofRequest(session);

        assertThatThrownBy(() -> handler.processCredentialRequest(request, responseBuilder, session))
                .hasMessage("Proof is missing")
                .asInstanceOf(InstanceOfAssertFactories.type(InvalidProofException.class))
                .extracting(OIDException::getErrorCode).isEqualTo("invalid_proof");

    }

    @DisplayName("Verify proof valid on credential request")
    @Test
    void test016() {
        var request = initValidKeyProofRequest(session, 2);

        handler.processCredentialRequest(request, responseBuilder, session);

        var expectedJwk = TestUtils.RELYING_PARTY_PUBLIC_KEY.toJSONString();
        assertThat(session.getCheckedParameters(SessionKey.VERIFIED_CREDENTIAL_KEYS)).isEqualTo(List.of(expectedJwk, expectedJwk));
    }

    static class InvalidProofType implements ProofType {

        @NotNull
        @Override
        public KClass<? extends Proof> getProofClass() {
            return null;
        }

        @NotNull
        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void register() {
            // Nothing to do for tests
        }

        @NotNull
        @Override
        public KSerializer<Proof> getProofsValueSerializer() {
            return null;
        }
    }
}
