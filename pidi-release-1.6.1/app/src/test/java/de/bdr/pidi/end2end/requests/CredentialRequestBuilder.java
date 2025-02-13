/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.requests;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.token.DPoPAccessToken;
import com.upokecenter.cbor.CBORObject;
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialFormat;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialFormat;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialFormat;
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialFormat;
import de.bdr.pidi.testdata.Pin;
import de.bdr.pidi.testdata.TestConfig;
import de.bdr.pidi.testdata.TestUtils;
import de.bundesdruckerei.mdoc.kotlin.core.SessionTranscript;
import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.http.HttpMethod;

public class CredentialRequestBuilder extends RequestBuilder<CredentialRequestBuilder> {
    private static final List<FlowVariant> authenticatedChannelVariants = List.of(FlowVariant.B, FlowVariant.B1);

    public static CredentialRequestBuilder validSdJwt(FlowVariant flowVariant, String dpopNonce, String accessToken) {
        return validSdJwt(flowVariant, TestUtils.DEVICE_KEY_PAIR, dpopNonce, accessToken);
    }

    public static CredentialRequestBuilder validSdJwt(FlowVariant flowVariant, ECKey deviceKeyPair, String dpopNonce, String accessToken) {
        final String format = authenticatedChannelVariants.contains(flowVariant) ?
                SdJwtVcAuthChannelCredentialFormat.INSTANCE.getFormat() :
                SdJwtVcCredentialFormat.INSTANCE.getFormat();
        var body = objectMapper.createObjectNode()
                .put("format", format)
                .put("vct", TestUtils.SD_JWT_VCTYPE);
        var path = getCredentialPath(flowVariant);
        var builder = new CredentialRequestBuilder(path)
                .withContentType("application/json; charset=utf-8")
                .withDpopHeader(flowVariant, deviceKeyPair, accessToken, dpopNonce)
                .withJsonBody(body)
                .withAccessToken(accessToken);
        if (authenticatedChannelVariants.contains(flowVariant)) {
            builder.withVerifierPub();
        }
        return builder;
    }

    public static CredentialRequestBuilder validMdoc(FlowVariant flowVariant, String dpopNonce, String accessToken) {
        return validMdoc(flowVariant, TestUtils.DEVICE_KEY_PAIR, dpopNonce, accessToken);
    }

    public static CredentialRequestBuilder validMdoc(FlowVariant flowVariant, ECKey deviceKeyPair, String dpopNonce, String accessToken) {
        final String format = authenticatedChannelVariants.contains(flowVariant) ?
                MsoMdocAuthChannelCredentialFormat.INSTANCE.getFormat() :
                MsoMdocCredentialFormat.INSTANCE.getFormat();
        var body = objectMapper.createObjectNode()
                .put("format", format)
                .put("doctype", "eu.europa.ec.eudi.pid.1");
        var path = getCredentialPath(flowVariant);
        var builder = new CredentialRequestBuilder(path)
                .withContentType("application/json; charset=utf-8")
                .withDpopHeader(flowVariant, deviceKeyPair, accessToken, dpopNonce)
                .withJsonBody(body)
                .withAccessToken(accessToken);
        if (authenticatedChannelVariants.contains(flowVariant)) {
            builder
                    .withVerifierPub()
                    .withSessionTranscript();
        }
        return builder;
    }

    public static CredentialRequestBuilder validSeedPid(FlowVariant flowVariant, ECKey deviceKeyPair, String dpopNonce, String accessToken, Pin pin) {
        var body = objectMapper.createObjectNode()
                .put("format", "seed_credential")
                .put("pin_derived_eph_key_pop", pin.pinDerivedEphKeyPop().serialize());
        var path = getCredentialPath(flowVariant);
        return new CredentialRequestBuilder(path)
                .withContentType("application/json; charset=utf-8")
                .withSeedProof(pin)
                .withDpopHeader(flowVariant, deviceKeyPair, accessToken, dpopNonce)
                .withAccessToken(accessToken)
                .withJsonBody(body)
                ;
    }

    public static ObjectNode validMdocRequestBody() {
        return objectMapper.createObjectNode()
                .put("format", "mso_mdoc")
                .put("doctype", "eu.europa.ec.eudi.pid.1");
    }

    public CredentialRequestBuilder() {
        super(HttpMethod.POST);
    }

    public CredentialRequestBuilder(String url) {
        this();
        withUrl(url);
    }

    public static String getCredentialPath(FlowVariant flowVariant) {
        return "/%s/credential".formatted(flowVariant.urlPath);
    }

    public static URI getCredentialUri(FlowVariant flowVariant) {
        return URI.create(TestConfig.pidiBaseUrl() + getCredentialPath(flowVariant));
    }

    public static String getAudience(FlowVariant flowVariant) {
        return TestConfig.pidiBaseUrl() + "/" + flowVariant.urlPath;
    }

    public CredentialRequestBuilder withFormatAndVct(String format, String vct) {
        withoutJsonBody();
        var body = objectMapper.createObjectNode().put("format", format);
        if (vct != null) {
            body.put("vct", vct);
        }
        withJsonBody(body);
        return this;
    }

    public CredentialRequestBuilder withSeedProof(Pin pin) {
        var body = buildProofBody(pin.deviceKeyProof());
        withJsonBody(body);
        return this;
    }

    /**
     * @return proof with not parsable jwt
     */
    public CredentialRequestBuilder withInvalidProof() {
        var body = buildProofBody("not parseable String");
        withJsonBody(body);
        return this;
    }

    /**
     * @return proof with invalid signature
     */
    public CredentialRequestBuilder withInvalidProof(FlowVariant flowVariant, String clientId, Instant jwtIssueTime, String jwtNonce) {
        var jwt = TestUtils.buildInvalidProofJwt(TestUtils.RELYING_PARTY_KEY_PAIR, clientId, getAudience(flowVariant), jwtIssueTime, jwtNonce);
        var body = buildProofBody(jwt);
        withJsonBody(body);
        return this;
    }

    public CredentialRequestBuilder withProof(FlowVariant flowVariant, String clientId, String jwtAudience, Instant jwtIssueTime, String jwtNonce) {
        var jwt = TestUtils.buildProofJwt(clientId, jwtAudience + "/" + flowVariant.urlPath, jwtIssueTime, jwtNonce);
        var body = buildProofBody(jwt);
        withJsonBody(body);
        return this;
    }

    public CredentialRequestBuilder withProof(FlowVariant flowVariant, String clientId, Instant jwtIssueTime, String jwtNonce, String differentJwtType) {
        var jwt = TestUtils.buildProofJwt(new JOSEObjectType(differentJwtType), clientId, getAudience(flowVariant), jwtIssueTime, jwtNonce);
        var body = buildProofBody(jwt);
        withJsonBody(body);
        return this;
    }

    public CredentialRequestBuilder withProofs(FlowVariant flowVariant, String clientId, Instant jwtIssueTime, String jwtNonce, int count) {
        var jwts = IntStream.range(0, count).mapToObj(i -> TestUtils.buildProofJwt(clientId, getAudience(flowVariant), jwtIssueTime, jwtNonce)).toList();
        var body = buildProofsBody(jwts);
        withJsonBody(body);
        return this;
    }

    public CredentialRequestBuilder withDpopHeader(FlowVariant flowVariant, ECKey deviceKeyPair, String accessToken, String dpopNonce) {
        final SignedJWT dpopProof;
        if (accessToken == null || accessToken.isBlank()) {
            dpopProof = TestUtils.getDpopProof(deviceKeyPair, httpMethod, getCredentialUri(flowVariant), dpopNonce);
        } else {
            dpopProof = TestUtils.getDpopProof(deviceKeyPair, httpMethod, getCredentialUri(flowVariant), accessToken, dpopNonce);
        }
        withHeader("dpop", dpopProof.serialize());
        return this;
    }

    public CredentialRequestBuilder withDpopHeader(FlowVariant flowVariant, String accessToken) {
        final SignedJWT dpopProof = TestUtils.getDpopProof(TestUtils.DEVICE_KEY_PAIR, HttpMethod.POST, getCredentialUri(flowVariant), new DPoPAccessToken(accessToken), null, null, null);
        withHeader("dpop", dpopProof.serialize());
        return this;
    }

    public CredentialRequestBuilder withVerifierPub() {
        ObjectNode node = objectMapper.createObjectNode().set("verifier_pub", objectMapper.valueToTree(TestUtils.RELYING_PARTY_PUBLIC_KEY.toJSONObject()));
        withJsonBody(node);
        return this;
    }

    public CredentialRequestBuilder withSessionTranscript() {
        var sessionTranscript = new SessionTranscript(null, null, CBORObject.FromObject("handover").EncodeToBytes());
        ObjectNode node = objectMapper.createObjectNode().put("session_transcript", Base64.getUrlEncoder().encodeToString(sessionTranscript.asCBOR().EncodeToBytes()));
        withJsonBody(node);
        return this;
    }

    private ObjectNode buildProofBody(SignedJWT jwt) {
        return buildProofBody(jwt.serialize());
    }

    private ObjectNode buildProofBody(String jwt) {
        var body = objectMapper.createObjectNode();
        var proof = body.putObject("proof");
        proof.put("jwt", jwt).put("proof_type", "jwt");
        return body;
    }

    private ObjectNode buildProofsBody(List<SignedJWT> jwts) {
        var body = objectMapper.createObjectNode();
        var proofs = body.putObject("proofs");
        var jwtArr = proofs.putArray("jwt");
        jwts.stream().map(JWSObject::serialize).forEach(jwtArr::add);
        return body;
    }
}
