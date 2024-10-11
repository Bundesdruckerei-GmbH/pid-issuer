/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
import de.bdr.pidi.authorization.core.domain.PresentationSigningRequest;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.particle.AuthorizationHandler;
import de.bdr.pidi.authorization.core.particle.CNonceIssuanceHandler;
import de.bdr.pidi.authorization.core.particle.ClientIdMatchHandler;
import de.bdr.pidi.authorization.core.particle.CredentialHandler;
import de.bdr.pidi.authorization.core.particle.DeviceKeyHandler;
import de.bdr.pidi.authorization.core.particle.DpopHandler;
import de.bdr.pidi.authorization.core.particle.ExpirationValidationHandler;
import de.bdr.pidi.authorization.core.particle.FinishAuthorizationHandler;
import de.bdr.pidi.authorization.core.particle.KeyProofHandler;
import de.bdr.pidi.authorization.core.particle.ParHandler;
import de.bdr.pidi.authorization.core.particle.PkceHandler;
import de.bdr.pidi.authorization.core.particle.PresentationSigningHandler;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class C2FlowController extends BaseFlowController {
    private static final FlowVariant FLOW_VARIANT = FlowVariant.C2;
    private static final List<Class<? extends CredentialRequest>> requestsUsingProof = List.of();

    public C2FlowController(SessionManager sm,
                            ClientConfigurationService clientConfigurationService,
                            AuthorizationConfiguration authorizationConfiguration,
                            NonceService nonceService,
                            /*
                            PIDI-1688: Temporarily disable Client Attestation
                            WalletAttestationService walletAttestationService,
                            */
                            IdentificationApi identificationProvider,
                            SdJwtBuilder<SdJwtVcCredentialRequest> c2SdJwtBuilder,
                            MdocBuilder<MsoMdocCredentialRequest> cMdocBuilder,
                            PidSerializer pidSerializer,
                            KeyProofService keyProofService) {
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
                        new KeyProofHandler(keyProofService, requestsUsingProof, false),
                        new DeviceKeyHandler(),
                        /*
                        Flow (process, issuance)
                         */
                        new ParHandler(authorizationConfiguration.getRequestUriLifetime()),
                        new AuthorizationHandler(authorizationConfiguration.getBaseUrl(), identificationProvider),
                        new FinishAuthorizationHandler(authorizationConfiguration.getAuthorizationCodeLifetime()),
                        new TokenHandler(authorizationConfiguration.getAccessTokenLifetime(), authorizationConfiguration.getAuthorizationScheme()),
                        new CredentialHandler(c2SdJwtBuilder, cMdocBuilder, pidSerializer, requestsUsingProof, true),
                        new PresentationSigningHandler(),
                        /*
                        Post process
                         */
                        new CNonceIssuanceHandler(authorizationConfiguration.getAccessTokenLifetime())),
                FLOW_VARIANT);
    }

    @Override
    public WResponseBuilder processCredentialRequest(HttpRequest<CredentialRequest> request) {
        return processCredentialRequest(request, Requests.PRESENTATION_SIGNING_REQUEST);
    }

    @Override
    public WResponseBuilder processPresentationSigningRequest(HttpRequest<PresentationSigningRequest> request) {
        final WSession session;
        try {
            var authorization = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
            session = sm.loadByAccessToken(authorizationScheme, authorization, flowVariant);
        } finally {
            log.info(PROCESSING_MSG, "presentation signing", request.getBody());
        }

        var builder = new WResponseBuilder();
        try {
            doProcessPresentationSigningRequest(request, builder, session);
        } finally {
            sm.persistAndTerminate(session);
        }
        return builder;
    }
}
