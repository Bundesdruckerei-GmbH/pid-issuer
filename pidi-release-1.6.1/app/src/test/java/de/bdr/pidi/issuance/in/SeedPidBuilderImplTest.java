/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.in;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.SeedException;
import de.bdr.pidi.issuance.core.service.SeedPidService;
import de.bdr.pidi.issuance.core.service.SeedSigner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class SeedPidBuilderImplTest {

    static final String ISSUER_ID = "https://issuer.de/pid";
    static final String CREDENTIAL_ISSUER_IDENTIFIER = "https://issuer.de/b1";
    /*
     * {"alg":"ES256","kid":"key12"}
     */
    static final String DUMMY_HEADER = "eyJhbGciOiJFUzI1NiIsImtpZCI6ImtleTEyIn0K";
    /*
     * {"alg":"ES256","foo":"bar"}
     */
    static final String HEADER_WITHOUT_KEY_ID = "eyJhbGciOiJFUzI1NiIsImZvbyI6ImJhciJ9Cg";
    /*
     * {"alg":"ES256","kid":null}
     */
    static final String HEADER_WITH_NULL_KEY_ID = "eyJhbGciOiJFUzI1NiIsImtpZCI6bnVsbH0K";
    /*
     * {"alg":"ES256","kid":"key12","foo":"bar"}
     */
    static final String HEADER_WITH_EXTRA_KEY = "eyJhbGciOiJFUzI1NiIsImtpZCI6ImtleTEyIiwiZm9vIjoiYmFyIn0K";
    /*
     * {"alg":"HS256","kid":"key12"}
     */
    static final String HEADER_ALG_HS256 = "eyJhbGciOiJIUzI1NiIsImtpZCI6ImtleTEyIn0K";
    /*
     * {"cnf":{"jwk":23},"foo"=bar;}
     */
    static final String BODY_BAD_SYNTAX = "eyJjbmYiOnsiandrIjoyM30sImZvbyI9YmFyO30K";
    /*
     * {"iat":1724342349,"iss":"pidi.bundesdruckerei.de"
     */
    static final String BODY_MISSING_RIGHT_BRACKET = "eyJpYXQiOjE3MjQzNDIzNDksImlzcyI6InBpZGkuYnVuZGVzZHJ1Y2tlcmVpLmRlIgo";

    /*
     * {"alg": "ES256", "kid": "sdsig_2024_001"}
     */
    static final String JWS_SAMPLE = "eyJraWQiOiJzZHNpZ18yMDI0XzAwMSIsImFsZyI6IkVTMjU2In0.eyJpc3MiOiJodHRwOi8vcGlkaS5sb2NhbGhvc3QuYmRyLmRlOjgwODAvYzEiLCJjbmYiOnsiandrIjp7Imt0eSI6IkVDIiwidXNlIjoic2lnIiwiY3J2IjoiUC0yNTYiLCJraWQiOiJmZWQ3OTg2Mi1hZjM2LTRmZWUtOGU2NC04OWUzYzkxMDkxZWQiLCJ4IjoibHh2R0NTbk9WRldhcmtvUEkyeTJKRDlzOU5oY3Z6X1FnQ18yX0p4NE1YYyIsInkiOiJVMThUTW5lNVZqSXlRYWZMZmwyTGpIR283b1pQVjdGNmdNeUZUem5wVndjIiwiYWxnIjoiRVMyNTYifX0sInBpZF9kYXRhX2VuYyI6ImV5SnJhV1FpT2lKelpHVnVZMTh5TURJMFh6QXdNU0lzSW1WdVl5STZJa0V5TlRaSFEwMGlMQ0poYkdjaU9pSmthWElpZlEuLnpIS3MwbGJhNTRVb3pGUGUuTHVPVjJTLVlSd0NyZGpGVzRhZUxIcVYyb1NTZXU4LTdLY1Roa3JLVXAzRjNIaldlT1F1UUVMbkp6ZE5Wc01PQTRsanRJYzl3WGtOY3lqVU1wcUc1TEpCLUVucDNGbnprLUNJc1RoYTJnRjVaR3NTcFllTTRSZ1ZGa01BRVA3di1fWUszcEd5eXJ4ZExWRk9XaUZhaEhlUzJKbGNzWDRPSHF3dWRXQk93WWFvalktd240WFd0bVlvR1I5RV9MejJEbm5qNGVhbGlVdldlaHV4Q0lObVVhblhNMmtLd0FmMUdtQUo4amF1RE9jMXZackNhT0FhbkVjZEg2NEMxcU5ORFk4Q0ZxRlZRelZ3cnQwSDAzN1dlbFNicjVuR2dOZFFPLl8yYUItOHRRUlY1d0VHaTA4WXpBU1EiLCJleHAiOjE3NTA5NDEyMzgsImlhdCI6MTcxOTQwNTIzOH0.J1L2NX3cDdTAbhITl_s1a_ZrBpF1Ims0KZ9LVvVg5ILSbkptjvVd57ZxFRUux_s9KPTjV1ma6Ey017j8_Vv8NA";

    final String keyString = """
            {"kty":"EC","d":"6UbIT1MUoWRn3x-ccCF1MdqO7oyo3h-rbu_xGwRy1-k","crv":"P-256","kid":"wombat23",
            "x":"wZtVteUmFqs4RoBACPy_x76TtwNvD-AsblmyAZYjRG8","y":"i2cLdZEY4uMy1kvt3Xvm4IHGSsCqfsUNtpJh2PeQNUo"}""";

    @Mock
    SeedPidService service;
    @Mock
    SeedSigner signer;

    SeedPidBuilderImpl out;

    @BeforeEach
    void setup() {
        this.out = new SeedPidBuilderImpl(service);
    }

    void given_currentSigner() throws ParseException, JOSEException {
        when(service.currentSigner()).thenReturn(signer);
        when(signer.keyIdentifier()).thenReturn("key123");
        ECKey key = ECKey.parse(keyString);
        JWSSigner jwsSigner = new ECDSASigner(key);
        when(signer.signer()).thenReturn(jwsSigner);
    }

    @Test
    void when_build_then_ok() throws JOSEException, ParseException {
        var data = PidCredentialData.Companion.getTEST_DATA_SET();
        var deviceJwk = createJwk().toPublicJWK();
        var cb = new JWTClaimsSet.Builder();
        var cnf = JSONObjectUtils.newJSONObject();
        cnf.put("jwk", deviceJwk.toJSONObject());
        cb.claim("cnf", cnf);
        when(service.writeAsClaims(data, deviceJwk, ISSUER_ID))
                .thenReturn(cb.build());
        given_currentSigner();

        var result = out.build(data, deviceJwk, ISSUER_ID);
        if (log.isDebugEnabled()) {
            log.debug("serialized signed JWT: {}", result);
        }

        assertNotNull(result);
        SignedJWT jwt = SignedJWT.parse(result);
        assertEquals(JWSObject.State.SIGNED, jwt.getState());
        var read = jwt.getJWTClaimsSet().getJSONObjectClaim("cnf");
        assertNotNull(read);
        var key = JSONObjectUtils.getJSONObject(read, "jwk");
        assertNotNull(key);
    }

    @Test
    void when_build_2_then_ok() throws JOSEException, ParseException {
        var data = PidCredentialData.Companion.getTEST_DATA_SET();
        var clientInstanceKey = createJwk().toPublicJWK();
        var pinDerivedPublicKey = createJwk().toPublicJWK();
        var cb = new JWTClaimsSet.Builder();
        var cnf = JSONObjectUtils.newJSONObject();
        cnf.put("jwk", clientInstanceKey.toJSONObject());
        cnf.put("pin_derived_public_jwk", pinDerivedPublicKey.toJSONObject());
        cb.claim("cnf", cnf);
        when(service.writeAsClaims(data, clientInstanceKey, pinDerivedPublicKey, CREDENTIAL_ISSUER_IDENTIFIER))
                .thenReturn(cb.build());
        given_currentSigner();

        var result = out.build(data, clientInstanceKey, pinDerivedPublicKey, CREDENTIAL_ISSUER_IDENTIFIER);
        if (log.isDebugEnabled()) {
            log.debug("serialized signed JWT: {}", result);
        }

        assertNotNull(result);
        SignedJWT jwt = SignedJWT.parse(result);
        assertEquals(JWSObject.State.SIGNED, jwt.getState());
        var read = jwt.getJWTClaimsSet().getJSONObjectClaim("cnf");
        assertNotNull(read);
        var key = JSONObjectUtils.getJSONObject(read, "jwk");
        assertNotNull(key);
        var pin = JSONObjectUtils.getJSONObject(read, "pin_derived_public_jwk");
        assertNotNull(pin);
    }

    ECKey createJwk() throws JOSEException {
        return new ECKeyGenerator(Curve.P_256).generate();
    }

    @ParameterizedTest
    @ValueSource(strings = {"nodot", "malformed.header.AB03CD23423407",
            DUMMY_HEADER + "." + DUMMY_HEADER + "."}) // empty signature
    void given_malformedJwt_when_extractVerified_EncSeed_then_fail(String input) {
        var exception = assertThrows(SeedException.class, () -> out.extractVerifiedEncSeed(input, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            HEADER_WITHOUT_KEY_ID + "." + DUMMY_HEADER + ".AB03CD23423407",
            HEADER_WITH_NULL_KEY_ID + "." + DUMMY_HEADER + ".AB03CD23423407",
            HEADER_WITH_EXTRA_KEY + "." + DUMMY_HEADER + ".AB03CD23423407"})
    void given_jwtHeaderInvalid_when_extractVerified_EncSeed_then_fail(String input) {
        var exception = assertThrows(SeedException.class, () -> out.extractVerifiedEncSeed(input, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            DUMMY_HEADER + "." + BODY_BAD_SYNTAX + ".AB03CD23423407",
            DUMMY_HEADER + "." + BODY_MISSING_RIGHT_BRACKET + ".AB03CD23423407"})
    void given_malformedJwtClaims_when_extractVerified_EncSeed_then_fail(String input) {
        var impl = Mockito.spy(out);
        Mockito.doNothing().when(impl).verifySignature(any());

        var exception = assertThrows(SeedException.class, () -> impl.extractVerifiedEncSeed(input, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());
    }

    @ParameterizedTest
    @ValueSource(strings = {"nodot", "malformed.header.AB03CD23423407",
            DUMMY_HEADER + "." + DUMMY_HEADER + "."}) // empty signature
    void given_malformedJwt_when_extractVerified_PinSeed_then_fail(String input) {
        var exception = assertThrows(SeedException.class, () -> out.extractVerifiedPinSeed(input, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            HEADER_WITHOUT_KEY_ID + "." + DUMMY_HEADER + ".AB03CD23423407",
            HEADER_WITH_NULL_KEY_ID + "." + DUMMY_HEADER + ".AB03CD23423407",
            HEADER_WITH_EXTRA_KEY + "." + DUMMY_HEADER + ".AB03CD23423407"})
    void given_jwtHeaderInvalid_when_extractVerified_PincSeed_then_fail(String input) {
        var exception = assertThrows(SeedException.class, () -> out.extractVerifiedPinSeed(input, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            DUMMY_HEADER + "." + BODY_BAD_SYNTAX + ".AB03CD23423407",
            DUMMY_HEADER + "." + BODY_MISSING_RIGHT_BRACKET + ".AB03CD23423407"})
    void given_malformedJwtClaims_when_extractVerified_PinSeed_then_fail(String input) {
        var impl = Mockito.spy(out);
        Mockito.doNothing().when(impl).verifySignature(any());

        var exception = assertThrows(SeedException.class, () -> impl.extractVerifiedPinSeed(input, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());
    }

    @Test
    void given_badAlgorithm_when_verifySignature_then_fail() throws ParseException {
        var jws = SignedJWT.parse(Base64URL.from(JWS_SAMPLE).toJSONString());

        assertThrows(SeedException.class, () -> out.verifySignature(jws));
    }

    @Test
    void given_unknownKey_when_verifySignature_then_fail() throws ParseException, JOSEException {
        var jws = SignedJWT.parse(Base64URL.from(JWS_SAMPLE).toJSONString());
        ECKey key = ECKey.parse(keyString);
        JWSVerifier verifier = new ECDSAVerifier(key);
        when(service.verifierForKeyId("sdsig_2024_001")).thenReturn(verifier);

        var exception = assertThrows(SeedException.class, () -> out.verifySignature(jws));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());
    }

    @Test
    @Disabled("only used to provide test data")
    void json() {
        String s = """
            {"alg":"HS256","kid":"key12"}
            """;
        var b = Base64.getUrlEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
        log.debug("String {}, B64 {}", s, b);
        assertNotNull(b);
    }

}
