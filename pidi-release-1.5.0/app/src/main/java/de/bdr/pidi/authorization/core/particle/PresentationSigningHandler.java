/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.PresentationSigningRequest;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.base.PidServerException;
import org.apache.commons.lang3.StringUtils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

public class PresentationSigningHandler implements OidHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void processPresentationSigningRequest(HttpRequest<PresentationSigningRequest> request, WResponseBuilder response, WSession session) {
        String hashBytesEnc = request.getBody().hashBytesEnc();
        if (StringUtils.isBlank(hashBytesEnc)) {
            throw new InvalidRequestException("Hash bytes missing");
        }
        byte[] hashBytes = new Base64URL(hashBytesEnc).decode();
        if (hashBytes.length != 32) {
            throw new InvalidRequestException("Hash bytes invalid");
        }

        JWK deviceKeyPair = session.getCheckedParameterAsJwk(SessionKey.DEVICE_KEY_PAIR);
        final String signature;
        try {
            signature = sign(deviceKeyPair, hashBytes);
        } catch (JOSEException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new PidServerException("Could not sign with ephemeral device keypair", e);
        }
        session.removeParameter(SessionKey.DEVICE_KEY_PAIR);
        response.withJsonBody(objectMapper.createObjectNode().put("signature_bytes", signature));
    }

    private String sign(JWK deviceKeyPair, byte[] hashBytes) throws JOSEException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        String jcaAlgorithm = "NONEwithECDSA";
        Signature sigInstance = Signature.getInstance(jcaAlgorithm);
        sigInstance.initSign(deviceKeyPair.toECKey().toPrivateKey(), secureRandom);
        sigInstance.update(hashBytes);
        var signature = sigInstance.sign();
        var rsSignature = ECDSA.transcodeSignatureToConcat(
                signature,
                ECDSA.getSignatureByteArrayLength(JWSAlgorithm.ES256)
        );

        // use Base64URL encoding *without* padding as specified in RFC 7515 JSON Web Signature
        // chapter 2 - Terminology:
        // Base64url Encoding - Base64 encoding using the URL- and filename-safe character set
        //                      [...] with all trailing '=' characters omitted
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rsSignature);
    }
}
