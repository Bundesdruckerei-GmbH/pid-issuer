/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.requests;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.HttpMethod;

import java.util.Base64;

public class PresentationSigningRequestBuilder extends RequestBuilder<PresentationSigningRequestBuilder> {

    public PresentationSigningRequestBuilder(String path) {
        super(HttpMethod.POST);
        withUrl(path);
    }

    public static PresentationSigningRequestBuilder valid(String payload, String accessToken) {
        // only c2 flow has presentation signing endpoint
        var path = "c2/presentation-signing";
        var body = objectMapper.createObjectNode()
                .put("hash_bytes", generateHashBytes(payload));
        return new PresentationSigningRequestBuilder(path)
                .withContentType("application/json; charset=utf-8")
                .withAccessToken(accessToken)
                .withJsonBody(body)
                ;
    }
    public static String generateHashBytes(String payload) {
        // API input is Base64URL(SHA256(signingInput))
        var signingInput = getJwsObjectSigningInput(payload);
        var digest = DigestUtils.sha256(signingInput);
        return Base64.getUrlEncoder().encodeToString(digest);
    }

    public static byte[] getJwsObjectSigningInput(String payload) {
        // Prepare JWS Object
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
        JWSObject jwsObject = new JWSObject(
                header,
                new Payload(payload));
        return jwsObject.getSigningInput();
    }

}
