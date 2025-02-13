/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialRequest;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest;
import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidCredentialRequestException;
import de.bdr.pidi.authorization.core.service.PidSerializer;
import de.bdr.pidi.authorization.out.issuance.FaultyRequestParameterException;
import de.bdr.pidi.authorization.out.issuance.MdocBuilder;
import de.bdr.pidi.authorization.out.issuance.SdJwtBuilder;
import de.bdr.pidi.base.PidServerException;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialRequest;
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialRequest;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CredentialHandler implements OidHandler {
    // Response keys
    private static final String CREDENTIAL_SINGLE = "credential";
    private static final String CREDENTIAL_BATCH = "credentials";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SdJwtBuilder<?> sdJwtBuilder;
    private final MdocBuilder<?> mdocBuilder;
    private final PidSerializer pidSerializer;
    private final List<Class<? extends CredentialRequest>> requestsUsingProof;
    private final boolean useOwnDeviceKey;

    public CredentialHandler(SdJwtBuilder<?> sdJwtBuilder, MdocBuilder<?> mdocBuilder, PidSerializer pidSerializer, List<Class<? extends CredentialRequest>> requestsUsingProof) {
        this(sdJwtBuilder, mdocBuilder, pidSerializer, requestsUsingProof, false);
    }

    @Override
    public void processCredentialRequest(HttpRequest<CredentialRequest> request, WResponseBuilder response, WSession session) {
        // Validation not neccessary:
        // check format and doctype/vct is done by CredentialDto
        var credentialRequest = request.getBody();
        var credentials = requestsUsingProof.contains(credentialRequest.getClass()) ?
                buildCredentials(session, request.getBody()) :
                buildCredentialsWithoutKeyProof(session, credentialRequest);
        response.withJsonBody(buildJsonBody(session.containsParameter(SessionKey.VERIFIED_CREDENTIAL_KEYS), credentials));
    }

    private List<String> buildCredentials(WSession session, CredentialRequest credentialRequest) {
        var holderBindingKeys = session.getOptionalParameter(SessionKey.VERIFIED_CREDENTIAL_KEY)
                .map(List::of).map(c -> (Collection<String>) c)
                .orElseGet(() -> session.getCheckedParameters(SessionKey.VERIFIED_CREDENTIAL_KEYS));
        var identificationData = session.getCheckedParameter(SessionKey.IDENTIFICATION_DATA);

        return holderBindingKeys.stream().map(k -> buildCredential(credentialRequest, identificationData, k, session.getFlowVariant())).toList();
    }

    @SuppressWarnings("unchecked")
    private String buildCredential(CredentialRequest credentialRequest, String identificationData, String holderBindingKey, FlowVariant flowVariant) {
        var credentialData = pidSerializer.fromString(identificationData);
        try {
            return switch (credentialRequest) {
                case SdJwtVcCredentialRequest sdJwtVcCredentialRequest ->
                        ((SdJwtBuilder<SdJwtVcCredentialRequest>)sdJwtBuilder).build(credentialData, sdJwtVcCredentialRequest, holderBindingKey);
                case SdJwtVcAuthChannelCredentialRequest sdJwtVcAuthChannelCredentialRequest ->
                        ((SdJwtBuilder<SdJwtVcAuthChannelCredentialRequest>)sdJwtBuilder).build(credentialData, sdJwtVcAuthChannelCredentialRequest, holderBindingKey);
                case MsoMdocCredentialRequest msoMdocCredentialRequest ->
                        ((MdocBuilder<MsoMdocCredentialRequest>)mdocBuilder).build(credentialData, msoMdocCredentialRequest, holderBindingKey, flowVariant);
                case MsoMdocAuthChannelCredentialRequest msoMdocAuthChannelCredentialRequest ->
                        ((MdocBuilder<MsoMdocAuthChannelCredentialRequest>)mdocBuilder).build(credentialData, msoMdocAuthChannelCredentialRequest, holderBindingKey, flowVariant);
                default -> throw new PidServerException("Validated CredentialRequest is unknown");
            };
        } catch (ParseException e) {
            throw new PidServerException(SessionKey.IDENTIFICATION_DATA.getValue() + " could not be parsed", e);
        } catch (FaultyRequestParameterException e) {
            throw new InvalidCredentialRequestException(e.getMessage(), e);
        }
    }

    private List<String> buildCredentialsWithoutKeyProof(WSession session, CredentialRequest credentialRequest) {
        var identificationData = session.getCheckedParameter(SessionKey.IDENTIFICATION_DATA);
        final String holderBindingKey = useOwnDeviceKey ?
                session.getCheckedParameterAsJwk(SessionKey.DEVICE_KEY_PAIR).toPublicJWK().toJSONString() : null;
        return List.of(buildCredential(credentialRequest, identificationData, holderBindingKey, session.getFlowVariant()));
    }

    private ObjectNode buildJsonBody(boolean isBatchedRequest, List<String> credentials) {
        var body = objectMapper.createObjectNode();
        if (isBatchedRequest) {
            var bodyCredentials = body.putArray(CREDENTIAL_BATCH);
            credentials.forEach(bodyCredentials::add);
        } else {
            body.put(CREDENTIAL_SINGLE, credentials.getFirst());
        }
        return body;
    }
}
