/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core;

import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.domain.PresentationSigningRequest;
import de.bdr.pidi.authorization.core.particle.OidHandler;
import de.bdr.pidi.base.requests.SeedCredentialRequest;
import lombok.RequiredArgsConstructor;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
@RequiredArgsConstructor
public abstract class FlowController {

    public static final String OPERATION_NOT_SUPPORTED_MSG = "operation not supported in this flow";
    protected final SessionManager sm;
    private final List<OidHandler> oidHandlers;

    public abstract WResponseBuilder processPushedAuthRequest(HttpRequest<?> request);
    protected void doProcessPushedAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        oidHandlers.forEach(oidHandler -> oidHandler.processPushedAuthRequest(request, response, session));
    }

    public abstract WResponseBuilder processAuthRequest(HttpRequest<?> request);
    protected void doProcessAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        oidHandlers.forEach(oidHandler -> oidHandler.processAuthRequest(request, response, session, true));
    }

    public abstract WResponseBuilder processFinishAuthRequest(HttpRequest<?> request);
    protected void doProcessFinishAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        oidHandlers.forEach(oidHandler -> oidHandler.processFinishAuthRequest(request, response, session));
    }

    public abstract WResponseBuilder processTokenRequest(HttpRequest<?> request);
    protected void doProcessTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        oidHandlers.forEach(oidHandler -> oidHandler.processTokenRequest(request, response, session));
    }

    public WResponseBuilder processRefreshTokenRequest(HttpRequest<?> request) {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED_MSG);
    }
    protected void doProcessRefreshTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        oidHandlers.forEach(oidHandler -> oidHandler.processRefreshTokenRequest(request, response, session));
    }

    public WResponseBuilder processSeedCredentialTokenRequest(HttpRequest<?> request) {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED_MSG);
    }
    protected void doProcessSeedCredentialTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        oidHandlers.forEach(oidHandler -> oidHandler.processSeedCredentialTokenRequest(request, response, session));
    }

    public abstract WResponseBuilder processCredentialRequest(HttpRequest<CredentialRequest> request);
    protected void doProcessCredentialRequest(HttpRequest<CredentialRequest> request, WResponseBuilder response, WSession session) {
        oidHandlers.forEach(oidHandler -> oidHandler.processCredentialRequest(request, response, session));
    }

    public WResponseBuilder processSeedCredentialRequest(HttpRequest<SeedCredentialRequest> request) {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED_MSG);
    }
    protected void doProcessSeedCredentialRequest(HttpRequest<SeedCredentialRequest> request, WResponseBuilder response, WSession session) {
        oidHandlers.forEach(oidHandler -> oidHandler.processSeedCredentialRequest(request, response, session));
    }

    public WResponseBuilder processPresentationSigningRequest(HttpRequest<PresentationSigningRequest> request) {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED_MSG);
    }
    protected void doProcessPresentationSigningRequest(HttpRequest<PresentationSigningRequest> request, WResponseBuilder response, WSession session) {
        oidHandlers.forEach(oidHandler -> oidHandler.processPresentationSigningRequest(request, response, session));
    }

    public WResponseBuilder processSessionRequest(HttpRequest<?> request) {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED_MSG);
    }
}
