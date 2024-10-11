/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.PresentationSigningRequest;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.testdata.TestUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresentationSigningHandlerTest {

    private static final JWK DEVICE_KEY_PAIR = TestUtils.DEVICE_KEY_PAIR;
    private static final byte[] HASH_BYTES = "Teststring mit 32 Zeichen.......".getBytes(StandardCharsets.UTF_8);

    @Mock
    private HttpRequest<PresentationSigningRequest> httpRequest;

    private final PresentationSigningHandler handler = new PresentationSigningHandler();

    @Test
    void shouldProcess() throws JOSEException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        var hashByteEnc = Base64.getUrlEncoder().encodeToString(HASH_BYTES);
        PresentationSigningRequest presentationSigningRequest = new PresentationSigningRequest(hashByteEnc);
        when(httpRequest.getBody()).thenReturn(presentationSigningRequest);
        WResponseBuilder responseBuilder = new WResponseBuilder();
        WSession session = new WSessionImpl(FlowVariant.C2, TestUtils.randomSessionId());
        session.putParameter(SessionKey.DEVICE_KEY_PAIR, DEVICE_KEY_PAIR.toJSONString());

        handler.processPresentationSigningRequest(httpRequest, responseBuilder, session);
        ResponseEntity<JsonNode> jsonResponse = responseBuilder.buildJSONResponseEntity();
        JsonNode body = jsonResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("signature_bytes")).isInstanceOf(JsonNode.class).isNotNull();
        assertThat(verify(body.get("signature_bytes").asText(), DEVICE_KEY_PAIR, HASH_BYTES)).isTrue();
        assertThat(session.containsParameter(SessionKey.DEVICE_KEY_PAIR)).isFalse();
    }

    @Test
    void shouldCreateValidSignatureForJWS() throws JOSEException, java.text.ParseException {
        // Prepare JWS Object
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
        JWSObject jwsObject = new JWSObject(
                header,
                new Payload("Hello World"));

        // API input is Base64URL(SHA256(signingInput))
        var signingInput = jwsObject.getSigningInput();
        var digest = DigestUtils.sha256(signingInput);
        var hashByteEnc = Base64.getUrlEncoder().encodeToString(digest);
        PresentationSigningRequest presentationSigningRequest = new PresentationSigningRequest(hashByteEnc);

        // Prepare test
        when(httpRequest.getBody()).thenReturn(presentationSigningRequest);
        WResponseBuilder responseBuilder = new WResponseBuilder();
        WSession session = new WSessionImpl(FlowVariant.C2, TestUtils.randomSessionId());
        session.putParameter(SessionKey.DEVICE_KEY_PAIR, DEVICE_KEY_PAIR.toJSONString());

        // Process request and extract the signature
        handler.processPresentationSigningRequest(httpRequest, responseBuilder, session);
        ResponseEntity<JsonNode> jsonResponse = responseBuilder.buildJSONResponseEntity();
        JsonNode body = jsonResponse.getBody();
        var signature = Base64URL.from(body.get("signature_bytes").asText());

        // Verify that the signature can be used to form a valid JWS object
        var serializedComposedJWS = new String(signingInput, StandardCharsets.UTF_8) + "." + signature.toString();
        var verifier = new ECDSAVerifier(DEVICE_KEY_PAIR.toECKey().toECPublicKey());
        assertThat(JWSObject.parse(serializedComposedJWS).verify(verifier)).isTrue();
    }

    @Test
    void shouldThrowExceptionOnEmptyBody() {
        PresentationSigningRequest presentationSigningRequest = new PresentationSigningRequest();
        when(httpRequest.getBody()).thenReturn(presentationSigningRequest);
        WResponseBuilder responseBuilder = new WResponseBuilder();
        WSession session = new WSessionImpl(FlowVariant.C2, TestUtils.randomSessionId());

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> handler.processPresentationSigningRequest(httpRequest, responseBuilder, session));
        assertThat(exception.getMessage()).isEqualTo("Hash bytes missing");
    }

    @Test
    void shouldThrowExceptionHashLengthInvalid() {
        PresentationSigningRequest presentationSigningRequest = new PresentationSigningRequest(new String(new byte[31]));
        when(httpRequest.getBody()).thenReturn(presentationSigningRequest);
        WResponseBuilder responseBuilder = new WResponseBuilder();
        WSession session = new WSessionImpl(FlowVariant.C2, TestUtils.randomSessionId());

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> handler.processPresentationSigningRequest(httpRequest, responseBuilder, session));
        assertThat(exception.getMessage()).isEqualTo("Hash bytes invalid");
    }

    private static boolean verify(String signature, JWK deviceKeyPair, byte[] hashBytes) throws JOSEException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        String jcaAlgorithm = "NONEwithECDSA";
        Signature sigInstance = Signature.getInstance(jcaAlgorithm);
        sigInstance.initVerify(deviceKeyPair.toECKey().toPublicKey());
        sigInstance.update(hashBytes);

        // the endpoint provides base64url-encoded JWS Signature in "R+S encoding" (concatenation of the two curve points r and s)
        // JCA expects regular ASN.1/DER encoded signature, so we need to transform the signature before the verification
        var derSignature = ECDSA.transcodeSignatureToDER(Base64.getUrlDecoder().decode(signature));
        return sigInstance.verify(derSignature);
    }
}