/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.SessionNotFoundException;
import de.bdr.pidi.base.PidServerException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PidiSessionAdapter {
    private final PidiSessionRepository pidiSessionRepository;
    private final ObjectMapper objectMapper;

    public WSessionImpl init(FlowVariant flowVariant, Requests nextExpected, Duration sessionExpirationTime) {
        var entity = new PidiSessionEntity();
        entity.setFlow(flowVariant);
        entity.setNextExpectedRequest(nextExpected);
        setExpirationTime(entity, sessionExpirationTime);
        var saved = pidiSessionRepository.save(entity);
        return map(saved);
    }

    public WSessionImpl findByRequestUri(String requestUri) throws SessionNotFoundException {
        return pidiSessionRepository.findFirstByRequestUri(requestUri).map(this::map)
                .orElseThrow(SessionNotFoundException::new);
    }

    public WSessionImpl findByAuthorizationCode(String authorizationCode) throws SessionNotFoundException {
        return pidiSessionRepository.findFirstByAuthorizationCode(authorizationCode).map(this::map)
                .orElseThrow(SessionNotFoundException::new);
    }

    public WSessionImpl findByIssuerState(String issuerState) throws SessionNotFoundException {
        return pidiSessionRepository.findFirstByIssuerState(issuerState).map(this::map)
                .orElseThrow(SessionNotFoundException::new);
    }

    public WSessionImpl findByAccessToken(String accessToken) throws SessionNotFoundException {
        return pidiSessionRepository.findFirstByAccessToken(accessToken).map(this::map)
                .orElseThrow(SessionNotFoundException::new);
    }

    public WSessionImpl findByRefreshTokenDigest(String refreshTokenDigest) throws SessionNotFoundException {
        return pidiSessionRepository.findFirstByRefreshTokenDigest(refreshTokenDigest).map(this::map)
                .orElseThrow(SessionNotFoundException::new);
    }

    public Optional<WSessionImpl> findByPidIssuerSessionId(String pidIssuerSessionId) throws SessionNotFoundException {
        return pidiSessionRepository.findFirstByPidIssuerSessionId(pidIssuerSessionId).map(this::map);
    }

    @Transactional
    public void update(WSessionImpl wSession, Duration sessionExpirationTime) throws SessionNotFoundException {
        var entity = pidiSessionRepository.getReferenceById(wSession.getSessionId());
        setAllFields(wSession, entity);
        setExpirationTime(entity, sessionExpirationTime);
    }

    @Transactional
    public int deleteExpiredSessions() {
        return pidiSessionRepository.deleteAllByExpiresBefore(Instant.now());
    }

    @Transactional
    public void persistAndTerminate(WSessionImpl wSession) {
        var entity = pidiSessionRepository.getReferenceById(wSession.getSessionId());
        setAllFields(wSession, entity);
        entity.setExpires(Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

    private WSessionImpl map(PidiSessionEntity entity) {
        var session = new WSessionImpl(entity.getFlow(), entity.getId());
        session.setNextExpectedRequest(entity.getNextExpectedRequest());
        if (entity.getSession() == null) {
            return session;
        }
        try {
            var parameters = objectMapper.readValue(entity.getSession(), new TypeReference<HashMap<SessionKey, String>>() {});
            session.putParameters(parameters);
            return session;
        } catch (JsonProcessingException e) {
            throw new PidServerException("Could not deserialize session parameters for session " + entity.getId() + "\n" + entity.getSession(), e);
        }
    }

    private void setAllFields(WSessionImpl source, PidiSessionEntity target) {
        target.setFlow(source.getFlowVariant());
        target.setNextExpectedRequest(source.getNextExpectedRequest());
        target.setAuthorizationCode(source.getParameter(SessionKey.AUTHORIZATION_CODE));
        target.setRequestUri(source.getParameter(SessionKey.REQUEST_URI));
        target.setIssuerState(source.getParameter(SessionKey.ISSUER_STATE));
        target.setAccessToken(source.getParameter(SessionKey.ACCESS_TOKEN));
        target.setRefreshTokenDigest(source.getParameter(SessionKey.REFRESH_TOKEN_DIGEST));
        target.setPidIssuerSessionId(source.getParameter(SessionKey.PID_ISSUER_SESSION_ID));
        try {
            String params = objectMapper.writeValueAsString(source.getParameters());
            target.setSession(params);
        } catch (JsonProcessingException e) {
            throw new PidServerException("Could not serialize session parameters for session " + source.getSessionId(), e);
        }
    }

    private void setExpirationTime(PidiSessionEntity entity, Duration sessionExpirationTime) {
        /*
        Postgress timestamp contains only microseconds and no nanoseconds
         */
        entity.setExpires(Instant.now().plus(sessionExpirationTime).truncatedTo(ChronoUnit.MICROS));
    }
}
