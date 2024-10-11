/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.domain.SessionKey;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface WSession {
    void putParameter(SessionKey name, String value);
    void putParameter(SessionKey name, Instant value);
    void putParameters(SessionKey name, Collection<String> values);
    boolean containsParameter(SessionKey name);
    String getParameter(SessionKey name);
    Optional<String> getOptionalParameter(SessionKey name);
    String getCheckedParameter(SessionKey name);
    Instant getParameterAsInstant(SessionKey name);
    Instant getCheckedParameterAsInstant(SessionKey name);
    JWK getCheckedParameterAsJwk(SessionKey name);
    Collection<String> getCheckedParameters(SessionKey name);
    String removeParameter(SessionKey name);
    boolean isNextAllowedRequest(Requests request);
    FlowVariant getFlowVariant();
}
