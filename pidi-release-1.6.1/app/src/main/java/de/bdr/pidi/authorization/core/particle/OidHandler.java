/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.PresentationSigningRequest;
import de.bdr.pidi.base.requests.SeedCredentialRequest;

/**
 * A handler represents a certain aspect of the oid flow. Every method is bound to a specific request in the flow.
 * <p>
 * An implementation of a handler <b>must</b> be stateless. There is no guaranty about the lifecycle of a handler.
 * <p>
 * A state can be hold by the {@link WSession} object available in each method.
 */
public interface OidHandler {

    default void processPushedAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
    }

    default void processAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session, boolean referencesPushedAuthRequest) {
    }

    default void processFinishAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
    }

    default void processTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
    }

    default void processRefreshTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
    }

    default void processSeedCredentialTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
    }

    default void processCredentialRequest(HttpRequest<CredentialRequest> request, WResponseBuilder response, WSession session) {
    }

    default void processSeedCredentialRequest(HttpRequest<SeedCredentialRequest> request, WResponseBuilder response, WSession session) {
    }

    default void processPresentationSigningRequest(HttpRequest<PresentationSigningRequest> request, WResponseBuilder response, WSession session) {
    }
}
