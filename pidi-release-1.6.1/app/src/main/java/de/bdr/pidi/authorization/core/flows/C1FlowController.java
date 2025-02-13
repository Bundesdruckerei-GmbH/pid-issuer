/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.flows;

import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialRequest;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest;
import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.SessionManager;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionManagement;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.exception.SessionNotFoundException;
import de.bdr.pidi.authorization.core.particle.AuthorizationHandler;
import de.bdr.pidi.authorization.core.particle.CNonceIssuanceHandler;
import de.bdr.pidi.authorization.core.particle.ClientIdMatchHandler;
import de.bdr.pidi.authorization.core.particle.CredentialHandler;
import de.bdr.pidi.authorization.core.particle.DpopHandler;
import de.bdr.pidi.authorization.core.particle.ExpirationValidationHandler;
import de.bdr.pidi.authorization.core.particle.FinishAuthorizationHandler;
import de.bdr.pidi.authorization.core.particle.KeyProofHandler;
import de.bdr.pidi.authorization.core.particle.ParHandler;
import de.bdr.pidi.authorization.core.particle.PkceHandler;
import de.bdr.pidi.authorization.core.particle.RedirectUriHandler;
import de.bdr.pidi.authorization.core.particle.RefreshTokenIssuanceHandler;
import de.bdr.pidi.authorization.core.particle.RefreshTokenValidationHandler;
import de.bdr.pidi.authorization.core.particle.RequestOrderValidationHandler;
import de.bdr.pidi.authorization.core.particle.ScopeHandler;
import de.bdr.pidi.authorization.core.particle.StateHandler;
import de.bdr.pidi.authorization.core.particle.TokenHandler;
import de.bdr.pidi.authorization.core.service.KeyProofService;
import de.bdr.pidi.authorization.core.service.NonceService;
import de.bdr.pidi.authorization.core.service.PidSerializer;
import de.bdr.pidi.authorization.out.identification.IdentificationApi;
import de.bdr.pidi.authorization.out.issuance.MdocBuilder;
import de.bdr.pidi.authorization.out.issuance.SdJwtBuilder;
import de.bdr.pidi.authorization.out.issuance.SeedPidBuilder;
import de.bdr.pidi.clientconfiguration.ClientConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class C1FlowController extends BaseFlowController {
    private static final FlowVariant FLOW_VARIANT = FlowVariant.C1;
    private static final List<Class<? extends CredentialRequest>> requestsUsingProof = List.of(SdJwtVcCredentialRequest.class, MsoMdocCredentialRequest.class);

    public C1FlowController(SessionManager sm,
                            ClientConfigurationService clientConfigurationService,
                            AuthorizationConfiguration authorizationConfiguration,
                            NonceService nonceService,
                            /*
                            PIDI-1688: Temporarily disable Client Attestation
                            WalletAttestationService walletAttestationService,
                            */
                            IdentificationApi identificationProvider,
                            SdJwtBuilder<SdJwtVcCredentialRequest> c1SdJwtBuilder,
                            MdocBuilder<MsoMdocCredentialRequest> cMdocBuilder,
                            PidSerializer pidSerializer,
                            KeyProofService keyProofService,
                            SeedPidBuilder seedPidBuilder) {
        super(sm, authorizationConfiguration, List.of(
                        /*
                        Pre process (validation, DPOP, key generation)
                         */
                        new ClientIdMatchHandler(clientConfigurationService),
                        new RedirectUriHandler(),
                        new RequestOrderValidationHandler(),
                        new ExpirationValidationHandler(authorizationConfiguration.getAuthorizationScheme()),
                        new StateHandler(),
                        new PkceHandler(),
                        new ScopeHandler(FLOW_VARIANT),
                        /*
                        PIDI-1688: Temporarily disable Client Attestation
                        new ClientAttestationHandler(walletAttestationService, authorizationConfiguration.getCredentialIssuerIdentifier(FLOW_VARIANT)),
                        new DpopHandler(nonceService, authorizationConfiguration.getProofTimeTolerance(), authorizationConfiguration.getProofValidity(), authorizationConfiguration.getBaseUrl(), authorizationConfiguration.getAuthorizationScheme(), true),
                        */
                        new DpopHandler(nonceService, authorizationConfiguration.getProofTimeTolerance(), authorizationConfiguration.getProofValidity(), authorizationConfiguration.getBaseUrl(), authorizationConfiguration.getAuthorizationScheme(), false),
                        new KeyProofHandler(keyProofService, requestsUsingProof, authorizationConfiguration.getBatchIssuanceMaxSize()),
                        new RefreshTokenValidationHandler(seedPidBuilder, pidSerializer, authorizationConfiguration.getCredentialIssuerIdentifier(FLOW_VARIANT)),
                        /*
                        Flow (process, issuance)
                         */
                        new ParHandler(authorizationConfiguration.getRequestUriLifetime()),
                        new AuthorizationHandler(authorizationConfiguration.getBaseUrl(), identificationProvider),
                        new FinishAuthorizationHandler(authorizationConfiguration.getAuthorizationCodeLifetime()),
                        new TokenHandler(authorizationConfiguration.getAccessTokenLifetime(), authorizationConfiguration.getAuthorizationScheme()),
                        new CredentialHandler(c1SdJwtBuilder, cMdocBuilder, pidSerializer, requestsUsingProof),
                        /*
                        Post process
                         */
                        new CNonceIssuanceHandler(authorizationConfiguration.getAccessTokenLifetime()),
                        new RefreshTokenIssuanceHandler(seedPidBuilder, pidSerializer, authorizationConfiguration.getCredentialIssuerIdentifier(FLOW_VARIANT))),
                FLOW_VARIANT);
    }

    @Override
    public WResponseBuilder processRefreshTokenRequest(HttpRequest<?> request) {
        var refreshToken = request.getParameters().get("refresh_token");
        WSession session;
        try {
            session = sm.loadByRefreshToken(refreshToken);
        } catch (SessionNotFoundException e) {
            log.debug("No session by refresh token found, init new session", e);
            session = sm.initRefresh(flowVariant, refreshToken);
        } finally {
            log.info(PROCESSING_MSG, "refresh token", request.getParameters());
        }

        var builder = new WResponseBuilder();
        try {
            doProcessRefreshTokenRequest(request, builder, session);
            ((WSessionManagement) session).setNextExpectedRequest(Requests.CREDENTIAL_REQUEST);
        } finally {
            sm.persist(session);
        }
        return builder;
    }
}
