/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.testdata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.dpop.DPoPProofFactory;
import com.nimbusds.oauth2.sdk.id.JWTID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.DPoPAccessToken;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.upokecenter.cbor.CBORObject;
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialFormat;
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialRequest;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialFormat;
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest;
import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.common.vci.proofs.Proof;
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProof;
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProofType;
import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import de.bdr.pidi.base.PidDataConst;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialFormat;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialRequest;
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialFormat;
import de.bdr.pidi.base.requests.SdJwtVcAuthChannelCredentialRequest;
import de.bdr.pidi.base.requests.SeedCredentialFormat;
import de.bdr.pidi.base.requests.SeedCredentialRequest;
import de.bdr.pidi.end2end.requests.TestDPopProofFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.bdr.openid4vc.vci.data.Constants.PROOF_TYPE_OPENID4VCI_PROOF_JWT;
import static de.bdr.pidi.authorization.ConfigTestData.AUTH_CONFIG;
import static de.bdr.pidi.base.PidDataConst.SD_JWT_VCTYPE_PATH;
import static java.util.Optional.ofNullable;

@Slf4j
public class TestUtils {
    // misc
    public static final String NONCE_REGEX = "[a-zA-Z0-9]{22}";
    public static final String ID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    public static final String ISSUER_IDENTIFIER_AUDIENCE = TestConfig.pidiBaseUrl();
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final CBORMapper CBOR_MAPPER = new CBORMapper();
    public static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final byte[] SESSION_TRANSCRIPT = "sessionTranscript".getBytes(StandardCharsets.UTF_8);
    public static final String SD_JWT_VCTYPE = AUTH_CONFIG.getBaseUrl().toString() + SD_JWT_VCTYPE_PATH;

    // JWT Types
    public static final JOSEObjectType JWT_PROOF_TYPE = new JOSEObjectType(PROOF_TYPE_OPENID4VCI_PROOF_JWT);
    public static final JOSEObjectType JWT_PIN_TYPE = new JOSEObjectType("pin_derived_eph_key_pop");
    public static final JOSEObjectType JWT_DEVICE_TYPE = new JOSEObjectType("device_key_pop");

    // client / wallet
    public static final String PRIVATE_KEY_PATH = "/pidi-test.key";
    public static final PrivateKey CLIENT_PRIVATE_KEY = readClientPrivateKey();
    public static final JWK CLIENT_PUBLIC_KEY = generateClientPublicKey(CLIENT_PRIVATE_KEY);

    // device / client instance
    public static final ECKey DEVICE_KEY_PAIR = generateEcKey();
    public static final JWK DEVICE_PUBLIC_KEY = DEVICE_KEY_PAIR.toPublicJWK();

    // relying party
    public static final ECKey RELYING_PARTY_KEY_PAIR = generateEcKey();
    public static final JWK RELYING_PARTY_PUBLIC_KEY = RELYING_PARTY_KEY_PAIR.toPublicJWK();

    // pin (static keys for unit tests only, otherwise pinRetryCounterId will not be unique)
    private static final ECKey PIN_DERIVED_KEY_PAIR = generateEcKey();
    public static final JWK PIN_DERIVED_PUBLIC_KEY = PIN_DERIVED_KEY_PAIR.toPublicJWK();
    public static final String C_NONCE = RandomUtil.randomString();
    public static final SignedJWT PIN_DERIVED_EPH_KEY_POP = buildPinDerivedEphKeyPop(C_NONCE, FlowVariant.B1, DEVICE_PUBLIC_KEY, PIN_DERIVED_KEY_PAIR);
    public static final SignedJWT DEVICE_KEY_PROOF = buildSeedCredentialProof(C_NONCE, FlowVariant.B1, PIN_DERIVED_PUBLIC_KEY, DEVICE_KEY_PAIR);
    public static final SignedJWT DEVICE_KEY_POP = buildDeviceKeyPop(C_NONCE, FlowVariant.B1, PIN_DERIVED_PUBLIC_KEY, DEVICE_KEY_PAIR);

    // unknown / invalid
    public static final ECKey DIFFERENT_KEY_PAIR = generateEcKey();

    // -------------------------------- Miscellaneous ------------------------------------------------------------------

    public static ECKey generateEcKey() {
        try {
            int nextedInt = SECURE_RANDOM.nextInt();
            log.debug("~~~~ KeyGen, random int: {}, algo: {}, provider: {}", nextedInt, SECURE_RANDOM.getAlgorithm(), SECURE_RANDOM.getProvider());
            return new ECKeyGenerator(Curve.P_256).secureRandom(SECURE_RANDOM)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(RandomUtil.randomString())
                    .algorithm(JWSAlgorithm.ES256)
                    .generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public static SignedJWT buildJWT(JWTClaimsSet claims, JOSEObjectType type, ECKey keyPair) {
        return buildJWT(claims, type, keyPair, keyPair.toPublicJWK());
    }

    public static SignedJWT buildJWT(JWTClaimsSet claims, JOSEObjectType type, ECKey keyPair, JWK headerJwk) {
        var algorithm = JWSAlgorithm.parse(keyPair.getAlgorithm().getName());
        var header = new JWSHeader.Builder(algorithm)
                .jwk(headerJwk)
                .type(type)
                .build();
        var signedJWT = new SignedJWT(header, claims);
        try {
            var signer = new ECDSASigner(keyPair.toECKey());
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
        return signedJWT;
    }

    public static SignedJWT buildJWT(JWTClaimsSet claims, JOSEObjectType type, PrivateKey privateKey, JWK publicKey) {
        var header = new JWSHeader.Builder(JWSAlgorithm.PS256)
                .jwk(publicKey)
                .type(type)
                .build();
        var signedJWT = new SignedJWT(header, claims);
        try {
            var signer = new RSASSASigner(privateKey);
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
        return signedJWT;
    }

    // -------------------------------- Credential Request POP/Proof ---------------------------------------------------

    public static SignedJWT buildSeedCredentialProof(String nonce, FlowVariant flowVariant, JWK jwk, JWK keyPair) {
        var pinDerivedEphPub = new JWKWrapper(jwk.toJSONObject());
        var claims = new JWTClaimsSet.Builder()
                .issueTime(new Date())
                .audience(ISSUER_IDENTIFIER_AUDIENCE + "/" + flowVariant.urlPath)
                .claim("nonce", nonce)
                .claim("pin_derived_eph_pub", pinDerivedEphPub)
                .build();
        return buildJWT(claims, JWT_PROOF_TYPE, keyPair.toECKey());
    }

    public static SignedJWT buildPinDerivedEphKeyPop(String nonce, FlowVariant flowVariant, JWK jwk, JWK keyPair) {
        var deviceKey = new JWKWrapper(jwk.toJSONObject());
        var claims = new JWTClaimsSet.Builder()
                .audience(ISSUER_IDENTIFIER_AUDIENCE + "/" + flowVariant.urlPath)
                .claim("nonce", nonce)
                .claim("pid_issuer_session_id", nonce)
                .claim("device_key", deviceKey)
                .build();
        return buildJWT(claims, JWT_PIN_TYPE, keyPair.toECKey());
    }

    public static SignedJWT buildDeviceKeyPop(String nonce, FlowVariant flowVariant, JWK jwk, JWK keyPair) {
        var pinDerivedEphPub = new JWKWrapper(jwk.toJSONObject());
        var claims = new JWTClaimsSet.Builder()
                .audience(ISSUER_IDENTIFIER_AUDIENCE + "/" + flowVariant.urlPath)
                .claim("pid_issuer_session_id", nonce)
                .claim("pin_derived_eph_pub", pinDerivedEphPub)
                .build();
        return buildJWT(claims, JWT_DEVICE_TYPE, keyPair.toECKey());
    }

    public static SignedJWT buildProofJwt(String issuer, String audience, Instant issueTime, String nonce) {
        return buildProofJwt(JWT_PROOF_TYPE, issuer, audience, issueTime, nonce);
    }

    public static SignedJWT buildProofJwt(JOSEObjectType type, String issuer, String audience, Instant issueTime, String nonce) {
        var claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .issueTime(ofNullable(issueTime).map(Date::from).orElse(null))
                .claim("nonce", nonce)
                .build();

        return buildJWT(claims, type, RELYING_PARTY_KEY_PAIR);
    }

    /**
     * @return proof with different public key in header
     */
    public static SignedJWT buildInvalidProofJwt(ECKey deviceKeyPair, String issuer, String audience, Instant issueTime, String nonce) {
        var claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(issueTime))
                .claim("nonce", nonce)
                .build();

        return buildJWT(claims, JWT_PROOF_TYPE, deviceKeyPair, DIFFERENT_KEY_PAIR.toPublicJWK());
    }

    // -------------------------------- Client Attestation -------------------------------------------------------------


    public static String getValidClientAssertion(FlowVariant flowVariant) {
        return getValidClientAttestationJwt().serialize() + "~" + getValidClientAttestationPopJwt(flowVariant).serialize();
    }

    public static String getValidClientAssertion(FlowVariant flowVariant, String pidIssuerNonce) {
        return getValidClientAttestationJwt().serialize() + "~" + getValidClientAttestationPopJwt(flowVariant, pidIssuerNonce).serialize();
    }

    public static SignedJWT getValidClientAttestationJwt() {
        var now = Instant.now();
        return getClientAttestationJwt(
                ClientIds.validClientId().toString(),
                ClientIds.validClientId().toString(),
                now.plusSeconds(30L),
                now,
                now,
                "cnf",
                Map.of("jwk", getClientInstanceKeyMap()));
    }

    /**
     * @return client attestation signed by device key instead of client key
     */
    public static SignedJWT getInvalidClientAttestationJwt() {
        var now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .issuer(ClientIds.validClientId().toString())
                .subject(ClientIds.validClientId().toString())
                .expirationTime(Date.from(now.plusSeconds(30L)))
                .notBeforeTime(Date.from(now))
                .issueTime(Date.from(now))
                .claim("cnf", Map.of("jwk", getClientInstanceKeyMap()))
                .build();

        return buildJWT(claims, JOSEObjectType.JWT, DEVICE_KEY_PAIR);
    }

    public static SignedJWT getClientAttestationJwt(String issuer, String subject, Instant expirationTime, Instant notBeforeTime, Instant issueTime, String claimName, Object claimValue) {
        var claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .expirationTime(ofNullable(expirationTime).map(Date::from).orElse(null))
                .notBeforeTime(ofNullable(notBeforeTime).map(Date::from).orElse(null))
                .issueTime(ofNullable(issueTime).map(Date::from).orElse(null))
                .claim(claimName, claimValue)
                .build();
        return buildJWT(claims, JOSEObjectType.JWT, CLIENT_PRIVATE_KEY, CLIENT_PUBLIC_KEY);
    }

    public static SignedJWT getValidClientAttestationPopJwt(FlowVariant flowVariant) {
        return getValidClientAttestationPopJwt(flowVariant, null);
    }

    public static SignedJWT getValidClientAttestationPopJwt(FlowVariant flowVariant, String pidIssuerNonce) {
        var now = Instant.now();
        var claimsSetBuilder = new JWTClaimsSet.Builder()
                .issuer(ClientIds.validClientId().toString())
                .expirationTime(Date.from(now.plusSeconds(30L)))
                .notBeforeTime(Date.from(now))
                .issueTime(Date.from(now))
                .audience(ISSUER_IDENTIFIER_AUDIENCE + "/" + flowVariant.urlPath)
                .jwtID("test");
        if (pidIssuerNonce != null) {
            claimsSetBuilder.claim("pid_issuer_nonce", pidIssuerNonce);
        }
        var claims = claimsSetBuilder.build();

        return buildJWT(claims, JOSEObjectType.JWT, DEVICE_KEY_PAIR);
    }

    public static SignedJWT getClientAttestationPopJwt(FlowVariant flowVariant, String issuerIdentifier) {
        var now = Instant.now();
        return getClientAttestationPopJwt(
                ClientIds.validClientId().toString(),
                now.plusSeconds(30L),
                now,
                now,
                List.of(issuerIdentifier + "/" + flowVariant.urlPath),
                "test");
    }

    public static SignedJWT getClientAttestationPopJwt(String issuer, Instant expirationTime, Instant notBeforeTime, Instant issueTime, List<String> audience, String jwtId) {
        var claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .expirationTime(ofNullable(expirationTime).map(Date::from).orElse(null))
                .notBeforeTime(ofNullable(notBeforeTime).map(Date::from).orElse(null))
                .issueTime(ofNullable(issueTime).map(Date::from).orElse(null))
                .audience(audience)
                .jwtID(jwtId)
                .build();
        return buildJWT(claims, JOSEObjectType.JWT, DEVICE_KEY_PAIR);
    }

    public static Map<String, Object> getClientInstanceKeyMap() {
        return DEVICE_PUBLIC_KEY.toJSONObject();
    }

    // -------------------------------- DPOP ---------------------------------------------------------------------------

    public static SignedJWT getDpopProof(HttpMethod method, URI uri, String nonce) {
        return getDpopProof(DEVICE_KEY_PAIR, method, uri, null, nonce != null ? new Nonce(nonce) : null, null, null);
    }

    public static SignedJWT getDpopProof(ECKey deviceKeyPair, HttpMethod method, URI uri, String nonce) {
        return getDpopProof(deviceKeyPair, method, uri, null, nonce != null ? new Nonce(nonce) : null, null, null);
    }

    public static SignedJWT getDpopProof(HttpMethod method, URI uri, String nonce, JOSEObjectType type) {
        return getDpopProof(DEVICE_KEY_PAIR, method, uri, null, nonce != null ? new Nonce(nonce) : null, null, type);
    }

    public static SignedJWT getDpopProof(HttpMethod method, URI uri, String nonce, Date iat) {
        return getDpopProof(DEVICE_KEY_PAIR, method, uri, null, nonce != null ? new Nonce(nonce) : null, iat, null);
    }

    public static SignedJWT getDpopProof(HttpMethod method, URI uri, String accessToken, String nonce) {
        return getDpopProof(DEVICE_KEY_PAIR, method, uri, new DPoPAccessToken(accessToken), nonce != null ? new Nonce(nonce) : null, null, null);
    }

    public static SignedJWT getDpopProof(ECKey deviceKeyPair, HttpMethod method, URI uri, String accessToken, String nonce) {
        return getDpopProof(deviceKeyPair, method, uri, new DPoPAccessToken(accessToken), nonce != null ? new Nonce(nonce) : null, null, null);
    }

    public static SignedJWT getDpopProof(JWK signingJwk, HttpMethod method, URI uri, AccessToken accessToken, Nonce nonce, Date iat, JOSEObjectType type) {
        try {
            TestDPopProofFactory proofFactory = new TestDPopProofFactory(
                    signingJwk,
                    JWSAlgorithm.ES256,
                    type == null ? DPoPProofFactory.TYPE : type);

            return proofFactory.createDPoPJWT(new JWTID(DPoPProofFactory.MINIMAL_JTI_BYTE_LENGTH), method.name(), uri, iat == null ? new Date() : iat, accessToken, nonce);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    private static PrivateKey readClientPrivateKey() {
        try (var resource = TestUtils.class.getResourceAsStream(PRIVATE_KEY_PATH)) {
            Objects.requireNonNull(resource, "JWT Private Key could not be found");

            byte[] tmp = resource.readAllBytes();
            return decodePrivateKey(new String(tmp, StandardCharsets.UTF_8), "RSA");
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static JWK generateClientPublicKey(PrivateKey privateKey) {
        try {
            var rsaPrivateKey = (RSAPrivateCrtKey) privateKey;
            var rsaPublicKeySpec = new RSAPublicKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent());
            var publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(rsaPublicKeySpec);
            return new RSAKey.Builder(publicKey).build();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String removeBeginEnd(String pem) {
        pem = pem.replaceAll("-----BEGIN (.*)-----", "");
        pem = pem.replaceAll("-----END (.*)----", "");
        pem = pem.replaceAll("[\r\n]", "");
        return pem.trim();
    }

    private static byte[] toEncodedBytes(final String pemEncoded) {
        final String normalizedPem = removeBeginEnd(pemEncoded);
        return Base64.getDecoder().decode(normalizedPem);
    }

    private static PrivateKey decodePrivateKey(final String pemEncoded, final String algorithm) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encodedBytes = toEncodedBytes(pemEncoded);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedBytes);
        KeyFactory kf = KeyFactory.getInstance(algorithm);
        return kf.generatePrivate(keySpec);
    }

    public static CredentialRequest createSdJwtCredentialRequest() {
        return createSdJwtCredentialRequest((JwtProof) null);
    }

    public static CredentialRequest createSdJwtCredentialRequest(JwtProof proof) {
        return new SdJwtVcCredentialRequest(SdJwtVcCredentialFormat.INSTANCE, proof, Collections.emptyList(), null, SD_JWT_VCTYPE);
    }

    public static CredentialRequest createSdJwtCredentialRequest(List<Proof> proofs) {
        return new SdJwtVcCredentialRequest(SdJwtVcCredentialFormat.INSTANCE, null, proofs, null, SD_JWT_VCTYPE);
    }

    public static CredentialRequest createSdJwtAuthChannelCredentialRequest(JwtProof proof) {
        return new SdJwtVcAuthChannelCredentialRequest(SdJwtVcAuthChannelCredentialFormat.INSTANCE, proof, Collections.emptyList(), null, SD_JWT_VCTYPE, DEVICE_PUBLIC_KEY);
    }

    public static CredentialRequest createMdocCredentialRequest() {
        return new MsoMdocCredentialRequest(MsoMdocCredentialFormat.INSTANCE, null, Collections.emptyList(), null, PidDataConst.MDOC_TYPE);
    }

    public static CredentialRequest createMdocAuthChannelCredentialRequest() {
        return new MsoMdocAuthChannelCredentialRequest(MsoMdocAuthChannelCredentialFormat.INSTANCE, null, Collections.emptyList(), null, PidDataConst.MDOC_TYPE, DEVICE_PUBLIC_KEY, SESSION_TRANSCRIPT);
    }

    public static SeedCredentialRequest createSeedCredentialRequest() {
        return createSeedCredentialRequest(DEVICE_KEY_PROOF.serialize(), PIN_DERIVED_EPH_KEY_POP.serialize());
    }

    public static SeedCredentialRequest createSeedCredentialRequest(String deviceKeySignedNonce, String pinSignedNonce) {
        var proof = new JwtProof(deviceKeySignedNonce, JwtProofType.INSTANCE);
        return new SeedCredentialRequest(SeedCredentialFormat.INSTANCE, proof, Collections.emptyList(), null, pinSignedNonce);
    }

    public static long randomSessionId() {
        return new Random().nextLong();
    }

    public static String generateAuthorizationCode() {
        return RandomUtil.randomString();
    }

    public static String generateRequestUri() {
        return "urn:ietf:params:oauth:request_uri:" + RandomUtil.randomString();
    }

    public static String generateIssuerState() {
        return RandomUtil.randomString();
    }

    public static String generateAccessToken() {
        return RandomUtil.randomString();
    }

    public static String generateRefreshTokenDigest() {
        return HexFormat.of().formatHex(RandomUtil.randomString().getBytes());
    }

    @NotNull
    public static Function<String, JsonNode> toJsonNode() {
        return content -> {
            try {
                return OBJECT_MAPPER.readTree(content);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static List<String> getDisclosures(String decodedSignature) {
        return Arrays.stream(decodedSignature.substring(decodedSignature.indexOf('~') + 1).split("~")).toList();
    }

    public static DecodedMdocPidValue getDecodedMdocValue(byte[] cbor) {
        try {
            return CBOR_MAPPER.readValue(cbor, DecodedMdocPidValue.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public record DecodedMdocPidValue(String random, int digestID, String elementIdentifier, Object elementValue) {
    }

    public static Map<String, Object> readPidValues(byte[] decodedMDoc) throws IOException {
        JsonNode jsonNode = CBOR_MAPPER.readTree(decodedMDoc).findValue("eu.europa.ec.eudi.pid.1");
        ObjectReader reader = CBOR_MAPPER.readerFor(new TypeReference<List<byte[]>>() {
        });
        List<byte[]> list = reader.readValue(jsonNode);
        return list.stream().map(TestUtils::getDecodedMdocValue).collect(Collectors.toMap(
                TestUtils.DecodedMdocPidValue::elementIdentifier, TestUtils.DecodedMdocPidValue::elementValue));
    }

    public static Map<String, CBORObject> readMdocIssuerAuth(byte[] decodedMDoc) throws IOException {
        JsonNode jsonNode = CBOR_MAPPER.readTree(decodedMDoc).findValue("issuerAuth");
        CBORObject msoBstr = CBORObject.DecodeFromBytes(jsonNode.get(2).binaryValue());
        CBORObject mso = CBORObject.DecodeFromBytes(msoBstr.GetByteString());
        CBORObject validityInfo = mso.get("validityInfo");
        CBORObject status = mso.get("status");
        CBORObject statusList = status.get("status_list");
        CBORObject statusIndex = statusList.get("idx");
        CBORObject statusUri = statusList.get("uri");
        CBORObject signed = validityInfo.get("signed");
        CBORObject validFrom = validityInfo.get("validFrom");
        CBORObject validUntil = validityInfo.get("validUntil");
        return Map.of("signed", signed, "validFrom", validFrom, "validUntil", validUntil,"statusUri",statusUri,"statusIndex",statusIndex);
    }

    private record JWKWrapper(Object jwk) {
    }
}
