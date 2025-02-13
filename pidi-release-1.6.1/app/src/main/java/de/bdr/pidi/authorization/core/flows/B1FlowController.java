/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.flows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jwt.SignedJWT;
import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.SessionManager;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionManagement;
import de.bdr.pidi.authorization.core.domain.PidIssuerNonce;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.exception.InvalidClientException;
import de.bdr.pidi.authorization.core.particle.AuthorizationHandler;
import de.bdr.pidi.authorization.core.particle.CNonceIssuanceHandler;
import de.bdr.pidi.authorization.core.particle.ClientIdMatchHandler;
import de.bdr.pidi.authorization.core.particle.CredentialHandler;
import de.bdr.pidi.authorization.core.particle.DpopHandler;
import de.bdr.pidi.authorization.core.particle.ExpirationValidationHandler;
import de.bdr.pidi.authorization.core.particle.FinishAuthorizationHandler;
import de.bdr.pidi.authorization.core.particle.InitPinRetryCounterHandler;
import de.bdr.pidi.authorization.core.particle.KeyProofHandler;
import de.bdr.pidi.authorization.core.particle.ParHandler;
import de.bdr.pidi.authorization.core.particle.PkceHandler;
import de.bdr.pidi.authorization.core.particle.RedirectUriHandler;
import de.bdr.pidi.authorization.core.particle.RequestOrderValidationHandler;
import de.bdr.pidi.authorization.core.particle.ScopeHandler;
import de.bdr.pidi.authorization.core.particle.SeedCredentialIssuanceHandler;
import de.bdr.pidi.authorization.core.particle.SeedCredentialValidationHandler;
import de.bdr.pidi.authorization.core.particle.StateHandler;
import de.bdr.pidi.authorization.core.particle.TokenHandler;
import de.bdr.pidi.authorization.core.service.KeyProofService;
import de.bdr.pidi.authorization.core.service.NonceService;
import de.bdr.pidi.authorization.core.service.PidSerializer;
import de.bdr.pidi.authorization.core.service.PinProofService;
import de.bdr.pidi.authorization.core.service.PinRetryCounterService;
import de.bdr.pidi.authorization.core.util.PinUtil;
import de.bdr.pidi.authorization.out.identification.IdentificationApi;
import de.bdr.pidi.authorization.out.issuance.MdocBuilder;
import de.bdr.pidi.authorization.out.issuance.SdJwtBuilder;
import de.bdr.pidi.authorization.out.issuance.SeedPidBuilder;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialRequest;
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialRequest;
import de.bdr.pidi.base.requests.SeedCredentialRequest;
import de.bdr.pidi.clientconfiguration.ClientConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.List;

@Slf4j
@Service
public class B1FlowController extends BaseFlowController {
    private static final FlowVariant FLOW_VARIANT = FlowVariant.B1;
    private static final List<Class<? extends CredentialRequest>> requestsUsingProof = List.of(SdJwtVcAuthChannelCredentialRequest.class, SeedCredentialRequest.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public B1FlowController(SessionManager sm,
                            ClientConfigurationService clientConfigurationService,
                            AuthorizationConfiguration authConfig,
                            NonceService nonceService,
                            /*
                            PIDI-1855: Temporarily disable Client Attestation
                            WalletAttestationService walletAttestationService,
                            */
                            IdentificationApi identificationProvider,
                            SdJwtBuilder<SdJwtVcAuthChannelCredentialRequest> b1SdJwtBuilder,
                            MdocBuilder<MsoMdocAuthChannelCredentialRequest> bMdocBuilder,
                            PidSerializer pidSerializer,
                            KeyProofService keyProofService,
                            SeedPidBuilder seedPidBuilder,
                            PinProofService pinProofService,
                            PinRetryCounterService pinRetryCounterService) {
        super(sm, authConfig, List.of(
                        /*
                        Pre process (validation, DPOP, key generation)
                         */
                        new ClientIdMatchHandler(clientConfigurationService),
                        new RequestOrderValidationHandler(),
                        new RedirectUriHandler(),
                        new ExpirationValidationHandler(authConfig.getAuthorizationScheme()),
                        new StateHandler(),
                        new PkceHandler(),
                        new ScopeHandler(FLOW_VARIANT),
                        /*
                        PIDI-1855: Temporarily disable Client Attestation
                        new ClientAttestationHandler(walletAttestationService, authorizationConfiguration.getCredentialIssuerIdentifier(FLOW_VARIANT)),
                        new DpopHandler(nonceService, authConfig.getProofTimeTolerance(), authConfig.getProofValidity(), authConfig.getBaseUrl(), authConfig.getAuthorizationScheme(), true),
                        */
                        new DpopHandler(nonceService, authConfig.getProofTimeTolerance(), authConfig.getProofValidity(), authConfig.getBaseUrl(), authConfig.getAuthorizationScheme(), false),
                        new KeyProofHandler(keyProofService, requestsUsingProof),
                        new SeedCredentialValidationHandler(seedPidBuilder, pidSerializer, authConfig.getCredentialIssuerIdentifier(FLOW_VARIANT), pinRetryCounterService, pinProofService),
                        /*
                        Flow (process, issuance)
                         */
                        new ParHandler(authConfig.getRequestUriLifetime()),
                        new AuthorizationHandler(authConfig.getBaseUrl(), identificationProvider),
                        new InitPinRetryCounterHandler(pinProofService, keyProofService, pinRetryCounterService),
                        new FinishAuthorizationHandler(authConfig.getAuthorizationCodeLifetime()),
                        new TokenHandler(authConfig.getAccessTokenLifetime(), authConfig.getAuthorizationScheme()),
                        new CredentialHandler(b1SdJwtBuilder, bMdocBuilder, pidSerializer, requestsUsingProof),
                        new SeedCredentialIssuanceHandler(seedPidBuilder, pidSerializer, authConfig.getCredentialIssuerIdentifier(FLOW_VARIANT)),
                        /*
                        Post process
                         */
                        new CNonceIssuanceHandler(authConfig.getAccessTokenLifetime())),
                FLOW_VARIANT);
    }

    @Override
    public WResponseBuilder processTokenRequest(HttpRequest<?> request) {
        return processTokenRequest(request, Requests.SEED_CREDENTIAL_REQUEST);
    }

    @Override
    public WResponseBuilder processSeedCredentialTokenRequest(HttpRequest<?> request) {
        final String pidIssuerSessionId;
        try {
            SignedJWT deviceKeyPop = PinUtil.parseBody(request, "device_key_pop");
            pidIssuerSessionId = deviceKeyPop.getJWTClaimsSet().getStringClaim("pid_issuer_session_id");
        } catch (ParseException e) {
            log.info(PROCESSING_MSG, "seed credential token", request.getParameters());
            throw new InvalidClientException("device_key_pop could not be parsed", e);
        }

        final WSession session;
        try {
            session = sm.loadOrInitSessionId(flowVariant, pidIssuerSessionId);
        } finally {
            log.info(PROCESSING_MSG, "seed credential token", request.getParameters());
        }

        var builder = new WResponseBuilder();
        try {
            doProcessSeedCredentialTokenRequest(request, builder, session);
            ((WSessionManagement) session).setNextExpectedRequest(Requests.CREDENTIAL_REQUEST);
        } finally {
            sm.persist(session);
        }
        return builder;
    }

    @Override
    public WResponseBuilder processSeedCredentialRequest(HttpRequest<SeedCredentialRequest> request) {
        final WSession session;
        try {
            var authorization = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
            session = sm.loadByAccessToken(authorizationScheme, authorization, flowVariant);
        } finally {
            log.info(PROCESSING_MSG, "seed credential", request.getParameters());
        }

        var builder = new WResponseBuilder();
        try {
            doProcessSeedCredentialRequest(request, builder, session);
        } finally {
            sm.persist(session);
        }
        return builder;
    }

    @Override
    public WResponseBuilder processSessionRequest(HttpRequest<?> request) {
        final PidIssuerNonce sessionIdNonce;
        try {
            sessionIdNonce = sm.createSessionIdNonce();
        } finally {
            log.info(PROCESSING_MSG, "pidi session", request.getParameters());
        }

        var builder = new WResponseBuilder();
        ObjectNode body = objectMapper.createObjectNode()
                .put("session_id", sessionIdNonce.getNonce().nonce())
                .put("session_id_expires_in", sessionIdNonce.getNonce().expiresIn().toSeconds());
        builder.withJsonBody(body);
        return builder;
    }
}
