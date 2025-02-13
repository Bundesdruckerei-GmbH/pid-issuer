/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.flows;

import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.SessionManager;
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
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialRequest;
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialRequest;
import de.bdr.pidi.clientconfiguration.ClientConfigurationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BFlowController extends BaseFlowController {
    private static final FlowVariant FLOW_VARIANT = FlowVariant.B;
    private static final List<Class<? extends CredentialRequest>> requestsUsingProof  = List.of(SdJwtVcAuthChannelCredentialRequest.class);

    public BFlowController(SessionManager sm,
                           ClientConfigurationService clientConfigurationService,
                           AuthorizationConfiguration authorizationConfiguration,
                           NonceService nonceService,
                           /*
                           PIDI-1688: Temporarily disable Client Attestation
                           WalletAttestationService walletAttestationService,
                           */
                           IdentificationApi identificationProvider,
                           SdJwtBuilder<SdJwtVcAuthChannelCredentialRequest> bSdJwtBuilder,
                           MdocBuilder<MsoMdocAuthChannelCredentialRequest> bMdocBuilder,
                           PidSerializer pidSerializer,
                           KeyProofService keyProofService) {
        super(sm, authorizationConfiguration, List.of(
                        /*
                        Validation
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
                        new KeyProofHandler(keyProofService, requestsUsingProof),
                        /*
                        Flow (process, issuance)
                         */
                        new ParHandler(authorizationConfiguration.getRequestUriLifetime()),
                        new AuthorizationHandler(authorizationConfiguration.getBaseUrl(), identificationProvider),
                        new FinishAuthorizationHandler(authorizationConfiguration.getAuthorizationCodeLifetime()),
                        new TokenHandler(authorizationConfiguration.getAccessTokenLifetime(), authorizationConfiguration.getAuthorizationScheme()),
                        new CredentialHandler(bSdJwtBuilder, bMdocBuilder, pidSerializer, requestsUsingProof),
                        /*
                        Post process
                         */
                        new CNonceIssuanceHandler(authorizationConfiguration.getAccessTokenLifetime())),
                FLOW_VARIANT);
    }
}
