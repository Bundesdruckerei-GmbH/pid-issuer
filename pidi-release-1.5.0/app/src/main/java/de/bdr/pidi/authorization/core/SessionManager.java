/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.domain.PidIssuerNonce;

public interface SessionManager {

    WSession init(FlowVariant variant);

    WSession initRefresh(FlowVariant variant, String refreshToken);
    WSession loadOrInitSessionId(FlowVariant variant, String pidIssuerSessionId);

    /**
     * load the session given the requestUri from the PAR endpoint.
     *
     * @return a new instance of the {@link WSession}
     * @throws de.bdr.pidi.authorization.core.exception.InvalidRequestException if the requestUri is malformed
     */
    WSession loadByRequestUri(String requestUri, FlowVariant variant);

    /**
     * @param code
     * @param variant
     * @return a new instance of the {@link WSession}
     */
    WSession loadByAuthorizationCode(String code, FlowVariant variant);

    /**
     * @param issuerState
     * @param variant
     * @return a new instance of the {@link WSession}
     */
    WSession loadByIssuerState(String issuerState, FlowVariant variant);

    /**
     * load the session when the request is not Flow-specific (eID)
     * @param issuerState
     * @return a new instance of the {@link WSession}
     */
    WSession loadByIssuerState(String issuerState);
    /**
     * @param authorization
     * @param variant
     * @return a new instance of the {@link WSession}
     */
    WSession loadByAccessToken(String scheme, String authorization, FlowVariant variant);

    /**
     * @param refreshTokenDigest
     * @return a new instance of the {@link WSession}
     */
    WSession loadByRefreshToken(String refreshTokenDigest);

    void persist(WSession session);

    void persistAndTerminate(WSession session);

    PidIssuerNonce createSessionIdNonce();
}
