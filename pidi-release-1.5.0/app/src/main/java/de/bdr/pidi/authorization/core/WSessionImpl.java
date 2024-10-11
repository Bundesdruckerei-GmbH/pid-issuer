/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.base.PidServerException;
import lombok.Getter;
import lombok.Setter;

import java.text.ParseException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;

public class WSessionImpl implements WSession, WSessionManagement {

    private static final String PARAMETER_NOT_FOUND_MSG = "%s not found";
    private static final String PARAMETER_NOT_A_LIST_MSG = "%s not a list";
    private static final String PARAMETER_NOT_PARSABLE_JWK_MSG = "%s not a parsable jwk";
    private final long sessionId;
    private final FlowVariant flowVariant;
    private final Map<SessionKey, String> parameters = new EnumMap<>(SessionKey.class);
    @Getter @Setter
    private Requests nextExpectedRequest;
    private final ObjectMapper mapper = new ObjectMapper();

    public WSessionImpl(FlowVariant flowVariant, long sessionId) {
        this.sessionId = sessionId;
        this.flowVariant = flowVariant;
    }

    protected WSessionImpl(WSessionImpl wSession) {
        this.sessionId = wSession.sessionId;
        this.flowVariant = wSession.flowVariant;
        this.parameters.putAll(wSession.parameters);
        this.nextExpectedRequest = wSession.nextExpectedRequest;
    }

    @Override
    public boolean isNextAllowedRequest(Requests request) {
        return nextExpectedRequest == request;
    }

    @Override
    public long getSessionId() {
        return sessionId;
    }

    @Override
    public FlowVariant getFlowVariant() {
        return flowVariant;
    }

    @Override
    public void putParameter(SessionKey name, String value) {
        parameters.put(name, value);
    }

    @Override
    public void putParameter(SessionKey name, Instant value) {
        var checked = ofNullable(value).map(Instant::toString).orElse(null);
        parameters.put(name, checked);
    }

    @Override
    public void putParameters(SessionKey name, Collection<String> values) {
        try {
            parameters.put(name, mapper.writeValueAsString(values));
        } catch (JsonProcessingException e) {
            throw new PidServerException(e.getMessage(), e);
        }
    }

    public void putParameters(Map<SessionKey, String> parameters) {
        this.parameters.putAll(parameters);
    }

    @Override
    public boolean containsParameter(SessionKey name) {
        return parameters.containsKey(name);
    }

    @Override
    public String getParameter(SessionKey name) {
        return parameters.get(name);
    }

    @Override
    public Optional<String> getOptionalParameter(SessionKey name) {
        return ofNullable(parameters.get(name));
    }

    @Override
    public String getCheckedParameter(SessionKey name) {
        return ofNullable(parameters.get(name))
                .filter(not(String::isBlank))
                .orElseThrow(() -> parameterNotFound(name));
    }

    @Override
    public Instant getParameterAsInstant(SessionKey name) {
        return ofNullable(parameters.get(name)).map(Instant::parse).orElse(null);
    }

    @Override
    public Instant getCheckedParameterAsInstant(SessionKey name) {
        return ofNullable(parameters.get(name))
                .map(Instant::parse)
                .orElseThrow(() -> parameterNotFound(name));
    }

    @Override
    public JWK getCheckedParameterAsJwk(SessionKey name) {
        return ofNullable(parameters.get(name))
                .map(p -> parseJwk(p, name))
                .orElseThrow(() -> parameterNotFound(name));
    }

    private JWK parseJwk(String value, SessionKey name) {
        try {
            return JWK.parse(value);
        } catch (ParseException e) {
            throw new PidServerException(PARAMETER_NOT_PARSABLE_JWK_MSG.formatted(name.getValue()), e);
        }
    }

    @Override
    public Collection<String> getCheckedParameters(SessionKey name) {
        return ofNullable(parameters.get(name))
                .map(p -> parseList(p, name))
                .orElseThrow(() -> parameterNotFound(name));
    }

    @Override
    public String removeParameter(SessionKey name) {
        return parameters.remove(Objects.requireNonNull(name, "Parameter 'name' must not be null"));
    }

    private Collection<String> parseList(String value, SessionKey name) {
        try {
            return mapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new PidServerException(PARAMETER_NOT_A_LIST_MSG.formatted(name.getValue()), e);
        }
    }

    private PidServerException parameterNotFound(SessionKey name) {
        return new PidServerException(PARAMETER_NOT_FOUND_MSG.formatted(name.getValue()));
    }

    public Map<SessionKey, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WSessionImpl wSession = (WSessionImpl) o;
        return Objects.equals(sessionId, wSession.sessionId) && Objects.equals(parameters, wSession.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, parameters);
    }
}
