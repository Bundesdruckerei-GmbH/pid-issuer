/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.service;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.AuthorizationConfiguration;
import de.bdr.pidi.authorization.core.AuthorizationHousekeeping;
import de.bdr.pidi.authorization.core.NonceFactory;
import de.bdr.pidi.authorization.core.SessionManager;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.Nonce;
import de.bdr.pidi.authorization.core.domain.PidIssuerNonce;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidGrantException;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.authorization.core.exception.SessionNotFoundException;
import de.bdr.pidi.authorization.core.exception.UnauthorizedException;
import de.bdr.pidi.authorization.core.util.DigestUtil;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import de.bdr.pidi.authorization.out.persistence.PidiNonceAdapter;
import de.bdr.pidi.authorization.out.persistence.PidiSessionAdapter;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SessionManagerImpl implements SessionManager, AuthorizationHousekeeping {
    private static final Pattern REQUEST_URI_PATTERN = Pattern.compile("urn:ietf:params:oauth:request_uri:[a-zA-Z\\d]{22}$");
    private static final String MDC_SESSION_ID = "sessionId";

    private final PidiSessionAdapter pidiSessionAdapter;
    private final PidiNonceAdapter pidiNonceAdapter;
    private final Duration pidiNonceLifetime;
    private final Duration sessionExpirationTime;

    public SessionManagerImpl(PidiSessionAdapter pidiSessionAdapter, PidiNonceAdapter pidiNonceAdapter, AuthorizationConfiguration config) {
        this.pidiSessionAdapter = pidiSessionAdapter;
        this.pidiNonceAdapter = pidiNonceAdapter;
        this.pidiNonceLifetime = config.getPidIssuerNonceLifetime();
        this.sessionExpirationTime = config.getSessionExpirationTime();
    }

    @Override
    public WSession init(FlowVariant variant) {
        var result = pidiSessionAdapter.init(variant, Requests.PUSHED_AUTHORIZATION_REQUEST, sessionExpirationTime);
        prepareMdc(result);
        return result;
    }

    @Override
    public WSession initRefresh(FlowVariant variant, String refreshToken) {
        validateRefreshToken(refreshToken);
        var result = pidiSessionAdapter.init(variant, Requests.TOKEN_REQUEST, sessionExpirationTime);

        String refreshTokenDigest = DigestUtil.computeDigest(refreshToken);
        result.putParameter(SessionKey.REFRESH_TOKEN_DIGEST, refreshTokenDigest);
        prepareMdc(result);
        return result;
    }

    @Override
    @Transactional
    public WSession loadOrInitSessionId(FlowVariant variant, String pidIssuerSessionId) {
        validateSessionId(pidIssuerSessionId);
        WSessionImpl result = pidiSessionAdapter.findByPidIssuerSessionId(pidIssuerSessionId)
                .orElseGet(() -> {
                    var sessionIdNonce = pidiNonceAdapter.findByNonce(pidIssuerSessionId, pidiNonceLifetime)
                            .orElseThrow(() -> new InvalidRequestException("Session ID is unknown"));
                    if (sessionIdNonce.isUsed() || Instant.now().isAfter(sessionIdNonce.getNonce().expirationTime())) {
                        throw new InvalidRequestException("Session ID invalid");
                    }
                    sessionIdNonce.setUsed(true);
                    pidiNonceAdapter.setUsed(sessionIdNonce);

                    WSessionImpl newInitSession = pidiSessionAdapter.init(variant, Requests.SEED_TOKEN_REQUEST, sessionExpirationTime);
                    newInitSession.putParameter(SessionKey.PID_ISSUER_SESSION_ID, sessionIdNonce.getNonce().nonce());
                    newInitSession.putParameter(SessionKey.PID_ISSUER_SESSION_ID_EXP_TIME, sessionIdNonce.getNonce().expirationTime());
                    return newInitSession;
                });
        prepareMdc(result);
        return result;
    }

    @Override
    public WSession loadByRequestUri(String requestUri, FlowVariant variant) {
        validateRequestUri(requestUri);
        var result = pidiSessionAdapter.findByRequestUri(requestUri);
        prepareMdc(result);
        return result;
    }

    private void validateRequestUri(String requestUri) {
        if (requestUri == null || !REQUEST_URI_PATTERN.matcher(requestUri).matches()) {
            throw new InvalidRequestException("invalid request_uri", "Invalid request_uri: " + requestUri);
        }
    }

    @Override
    public WSession loadByAuthorizationCode(String code, FlowVariant variant) {
        validateCode(code);
        var result = pidiSessionAdapter.findByAuthorizationCode(code);
        prepareMdc(result);
        return result;
    }

    private void validateCode(String code) {
        if (!RandomUtil.isValid(code)) {
            throw new InvalidGrantException("invalid authorization code");
        }
    }

    @Override
    public WSession loadByIssuerState(String issuerState, FlowVariant variant) {
        validateIssuerState(issuerState);
        var result = pidiSessionAdapter.findByIssuerState(issuerState);
        if (result.getFlowVariant() != variant) {
            throw new SessionNotFoundException();
        }
        prepareMdc(result);
        return result;
    }

    @Override
    public WSession loadByIssuerState(String issuerState) {
        validateIssuerState(issuerState);
        var result = pidiSessionAdapter.findByIssuerState(issuerState);
        prepareMdc(result);
        return result;
    }

    private void validateIssuerState(String issuerState) {
        if (!RandomUtil.isValid(issuerState)) {
            throw new InvalidRequestException("invalid issuer_state", "Invalid issuer_state: " + issuerState);
        }
    }

    @Override
    public WSession loadByAccessToken(String scheme, String authorization, FlowVariant variant) {
        var accessToken = validateAccessToken(scheme, authorization);
        WSessionImpl result;
        try {
            result = pidiSessionAdapter.findByAccessToken(accessToken);
        } catch (SessionNotFoundException e) {
            throw new UnauthorizedException(scheme, "session not found");
        }
        prepareMdc(result);
        if (result.getFlowVariant() != variant) {
            throw new UnauthorizedException(scheme);
        }
        return result;
    }

    private String validateAccessToken(String expectedScheme, String authorization) {
        if (authorization == null) {
            throw new UnauthorizedException(expectedScheme);
        }
        var parts = authorization.split(" ");
        if (parts.length != 2 || !parts[0].equals(expectedScheme)) {
            throw new UnauthorizedException(expectedScheme);
        }
        if (!RandomUtil.isValid(parts[1])) {
            throw new UnauthorizedException(expectedScheme, "invalid_token");
        } else {
            return parts[1];
        }
    }

    @Override
    public WSession loadByRefreshToken(String refreshToken) {
        validateRefreshToken(refreshToken);
        String refreshTokenDigest = DigestUtil.computeDigest(refreshToken);

        var result = pidiSessionAdapter.findByRefreshTokenDigest(refreshTokenDigest);
        prepareMdc(result);
        return result;
    }

    private static void validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRequestException("Refresh token missing");
        }
    }

    private static void validateSessionId(String pidIssuerSessionId) {
        if (pidIssuerSessionId == null || pidIssuerSessionId.isBlank()) {
            throw new InvalidRequestException("Session ID is missing");
        }
    }

    @Override
    public void persist(WSession session) {
        // TODO PIDI-235 make sure that changing the WSession#nextExpectedRequest can only be changed from one request - even if there are concurrent requests
        WSessionImpl sessionImpl = (WSessionImpl) session;
        pidiSessionAdapter.update(sessionImpl, sessionExpirationTime);
    }

    @Override
    public void persistAndTerminate(WSession session) {
        pidiSessionAdapter.persistAndTerminate((WSessionImpl) session);
    }

    @Override
    public void cleanupExpiredSessions() {
        var count = pidiSessionAdapter.deleteExpiredSessions();
        log.info("Deleted {} expired sessions", count);
    }

    @Override
    public PidIssuerNonce createSessionIdNonce() {
        Nonce nonce = NonceFactory.createSecureRandomNonce(pidiNonceLifetime);
        MDC.put(MDC_SESSION_ID, String.valueOf(nonce.nonce()));
        return pidiNonceAdapter.createAndSave(nonce, pidiNonceLifetime);
    }

    private static void prepareMdc(WSessionImpl result) {
        MDC.put(MDC_SESSION_ID, String.valueOf(result.getSessionId()));
        result.getOptionalParameter(SessionKey.CLIENT_ID).ifPresent(clientId -> MDC.put(SessionKey.CLIENT_ID.getValue(), clientId));
    }
}
