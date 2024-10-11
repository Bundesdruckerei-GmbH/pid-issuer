/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.flows;

import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialRequest;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest;
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
import de.bdr.pidi.clientconfiguration.ClientConfigurationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CFlowController extends BaseFlowController {
    private static final FlowVariant FLOW_VARIANT = FlowVariant.C;
    private static final List<Class<? extends CredentialRequest>> requestsUsingProof  = List.of(SdJwtVcCredentialRequest.class, MsoMdocCredentialRequest.class);

    public CFlowController(SessionManager sm,
                           ClientConfigurationService clientConfigurationService,
                           AuthorizationConfiguration authorizationConfiguration,
                           NonceService nonceService,
                           /*
                           PIDI-739: Temporarily disable Client Attestation
                           WalletAttestationService walletAttestationService,
                           */
                           IdentificationApi identificationProvider,
                           SdJwtBuilder<SdJwtVcCredentialRequest> cSdJwtBuilder,
                           MdocBuilder<MsoMdocCredentialRequest> cMdocBuilder,
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
                        PIDI-739: Temporarily disable Client Attestation
                        new ClientAttestationHandler(walletAttestationService, authorizationConfiguration.getCredentialIssuerIdentifier(FLOW_VARIANT)),
                        new DpopHandler(nonceService, authorizationConfiguration.getProofTimeTolerance(), authorizationConfiguration.getProofValidity(), authorizationConfiguration.getBaseUrl(), authorizationConfiguration.getAuthorizationScheme(), true),
                        */
                        new DpopHandler(nonceService, authorizationConfiguration.getProofTimeTolerance(), authorizationConfiguration.getProofValidity(), authorizationConfiguration.getBaseUrl(), authorizationConfiguration.getAuthorizationScheme(), false),
                        new KeyProofHandler(keyProofService, requestsUsingProof, true),
                        /*
                        Flow (process, issuance)
                         */
                        new ParHandler(authorizationConfiguration.getRequestUriLifetime()),
                        new AuthorizationHandler(authorizationConfiguration.getBaseUrl(), identificationProvider),
                        new FinishAuthorizationHandler(authorizationConfiguration.getAuthorizationCodeLifetime()),
                        new TokenHandler(authorizationConfiguration.getAccessTokenLifetime(), authorizationConfiguration.getAuthorizationScheme()),
                        new CredentialHandler(cSdJwtBuilder, cMdocBuilder, pidSerializer, requestsUsingProof),
                        /*
                        Post process
                         */
                        new CNonceIssuanceHandler(authorizationConfiguration.getAccessTokenLifetime())),
                FLOW_VARIANT);
    }
}
