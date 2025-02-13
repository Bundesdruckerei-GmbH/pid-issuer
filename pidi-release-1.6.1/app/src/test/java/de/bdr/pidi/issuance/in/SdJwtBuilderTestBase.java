/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.in;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

abstract class SdJwtBuilderTestBase extends PidCredentialDataBase {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @NotNull
    static Function<String, JsonNode> toJsonNode() {
        return content -> {
            try {
                return OBJECT_MAPPER.readTree(content);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @NotNull
    static ArrayList<String> getAllSdHashVales(String payload) throws JsonProcessingException {
        ArrayList<String> listSdHashes = new ArrayList<>();
        JsonNode jsonNode = OBJECT_MAPPER.readTree(payload);
        List<JsonNode> sdJsonNodes = jsonNode.findValues("_sd");
        sdJsonNodes.forEach(node -> node.forEach(value -> listSdHashes.add(value.textValue())));
        return listSdHashes;
    }

    static String getDecodedPayload(String sdJwt) {
        String[] split = sdJwt.split("\\.");
        byte[] decodedPayload = Base64.getUrlDecoder().decode(split[1]);
        return new String(decodedPayload);
    }

    static String getDecodedHeader(String sdJwt) {
        String[] split = sdJwt.split("\\.");
        byte[] decodedHeader = Base64.getUrlDecoder().decode(split[0]);
        return new String(decodedHeader);
    }

    static String getSignature(String sdJwt) {
        String[] split = sdJwt.split("\\.");
        return split[2];
    }

    static List<String> getDisclosures(String decodedSignature) {
        return Arrays.stream(decodedSignature.substring(decodedSignature.indexOf('~') + 1).split("~")).toList();
    }

    static List<String> toHashedDisclosures(List<String> disclosures, MessageDigest messageDigest) {
        return disclosures.stream().map(disclosure -> removePadding(Base64.getUrlEncoder().encodeToString(messageDigest.digest(disclosure.getBytes())))).toList();
    }

    private static String removePadding(String value) {
        return value.replace("=", "");
    }

    static void verifySignature(String sdJwt, JWSVerifier verifier) {
        try {
            SignedJWT jwt = SignedJWT.parse(stripDisclosures(sdJwt));
            assertThat(jwt.verify(verifier)).isTrue();
        } catch (java.text.ParseException | JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    static String stripDisclosures(String sdJwt) {
        return sdJwt.split("~", 2)[0];
    }
}
