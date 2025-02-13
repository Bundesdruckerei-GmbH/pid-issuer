/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.core.service;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.SeedException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ExtendWith(MockitoExtension.class)
class SeedPidServiceTest {

    static final String ENCRYPT_KEY_ID = "seed-encrypt-23";
    static final String ISSUER_ID = "https://issuer.de/pid";
    static final String JWS_SAMPLE_CLAIMS = "eyJpc3MiOiJodHRwczovL2lzc3Vlci5kZS9waWQiLCJjbmYiOnsia2V5Ijp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJraWQiOiJ3b21iYXQyMyIsIngiOiJ3WnRWdGVVbUZxczRSb0JBQ1B5X3g3NlR0d052RC1Bc2JsbXlBWllqUkc4IiwieSI6ImkyY0xkWkVZNHVNeTFrdnQzWHZtNElIR1NzQ3Fmc1VOdHBKaDJQZVFOVW8ifX0sInBpZF9kYXRhX2VuYyI6ImV5SnJhV1FpT2lKelpXVmtMV1Z1WTNKNWNIUXRNak1pTENKbGJtTWlPaUpCTWpVMlIwTk5JaXdpWVd4bklqb2laR2x5SW4wLi5CakxmMjZvcWdDRmk5NzN4Ll9jVGZJNjlFbVVQdlV1ZDFrb0ttemtMTldXU25hSWxxMmJ5cTRodEFvUUlQTHNOTzAyR2dPdW1FUlZORFF4aHhLWnhNclY4LUltMXdUdE1lYmZtTFBldEdBVWxQcERnbGdxNGlwZy1pZjhjeTRZWW1DOWhjOG1nTTBNNjRpZ01wRDY4dGZ4d1dtOFdGSFFRUUlnWGhlVXdJVUJuYlY5aDNmazU3amhYT185WGxYdUUyVFhpRW81MElFQkdyNjF5cDJkWnJlR2d0dUw0Zy1GSlhQY01zRE12d2YyMUhrTkgzNHNEYjg4eDZTY2JUWUhUSDE2d0ZSQ0ZGVlpjSllBZlNpUEdWblRSSHhVTEJSWTU0WEhaVUJPa2Ewc0dWaE8wLW9TNEhLNnlNckItbWx4M2FPRk9uYjZiTVJKc0c0aUEwQWJDdmVyTU1CVEQ2bVhnU2xrd0xocERiQ3BiNk1CaHMuWmt1cHQyUEhoSjZyNjFvWnExaEVNUSIsImV4cCI6MTcyMjA3MzM4MCwiaWF0IjoxNzE5NDgxMzgwfQ==";
    static SecretKey secretKey;
    static final String SECRET_KEY_B64 = "NxmmofkL9_6-PHVb-dT2iPI_ScthA4qCIadWshpLTxM=";
    static final String USER_KEY_STRING = """
            {"kty":"EC","d":"6UbIT1MUoWRn3x-ccCF1MdqO7oyo3h-rbu_xGwRy1-k","crv":"P-256","kid":"wombat23",
            "x":"wZtVteUmFqs4RoBACPy_x76TtwNvD-AsblmyAZYjRG8","y":"i2cLdZEY4uMy1kvt3Xvm4IHGSsCqfsUNtpJh2PeQNUo"}""";

    @Mock
    SeedTrustManager trustManager;

    SeedPidService out;

    @BeforeAll
    static void setupClass() {
        var bytes = Base64.getUrlDecoder().decode(SECRET_KEY_B64);
        secretKey = new SecretKeySpec(bytes, "AES");
    }

    @BeforeEach
    void setup() {
        out = new SeedPidService(trustManager, Duration.ofDays(14));
    }

    @SuppressWarnings("unchecked")
    @Test
    void given_defaultData_when_writeAsClaims_then_ok() throws ParseException {
        var pid = PidCredentialData.Companion.getTEST_DATA_SET();
        var jwk = given_userBindingKey();
        var issuerId = "https://pidi.bdr.de/c";
        given_seedEncrypter();

        var result = out.writeAsClaims(pid, jwk, issuerId);

        assertNotNull(result);
        assertEquals(issuerId, result.getStringClaim("iss"));

        var exp = result.getExpirationTime().toInstant();
        assertTrue(exp.isAfter(Instant.now()));

        var cnf = result.getJSONObjectClaim("cnf");
        var cnfJwk = cnf.get("jwk");
        assertThat(cnfJwk).isInstanceOf(Map.class);
        assertThat(JWK.parse((Map<String, Object>) cnfJwk)).isEqualTo(jwk);

        log.debug("payload: {}", result);
    }

    @SuppressWarnings("unchecked")
    @Test
    void given_defaultData_when_writeAsClaims_2_then_ok() throws ParseException {
        var pid = PidCredentialData.Companion.getTEST_DATA_SET();
        var cik = given_clientInstanceKey();
        var pdk = given_pinDerivedPublicKey();
        var issuerId = "https://pidi.bdr.de/c";

        var result = out.writeAsClaims(pid, cik, pdk, issuerId);

        assertThat(result).isNotNull();
        assertThat(result.getStringClaim("iss")).isEqualTo(issuerId);
        var data = result.getClaim(SeedPidService.DATA_JSON);
        assertThat(data)
                .hasFieldOrPropertyWithValue("family_name", pid.getFamilyName())
                .extracting("place_of_birth")
                .hasFieldOrPropertyWithValue("locality", Objects.requireNonNull(pid.getPlaceOfBirth()).getLocality());

        var exp = result.getExpirationTime().toInstant();
        assertThat(exp).isAfter(Instant.now());

        var cnf = result.getJSONObjectClaim("cnf");
        var cnfJwk = cnf.get("jwk");
        var cnfPin = cnf.get("pin_derived_public_jwk");
        assertThat(cnfJwk).isInstanceOf(Map.class);
        assertThat(JWK.parse((Map<String, Object>)cnfJwk)).isEqualTo(cik);
        assertThat(cnfPin).isInstanceOf(Map.class);
        assertThat(JWK.parse((Map<String, Object>)cnfPin)).isEqualTo(pdk);

        log.debug("payload: {}", result);
    }

    @Test
    void given_defaultData_when_writePidAsJwe_then_ok() {
        var pid = PidCredentialData.Companion.getTEST_DATA_SET();
        given_seedEncrypter();

        var result = out.writePidAsJwe(pid);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(JWEObject.State.ENCRYPTED, result.getState()),
                () -> assertNotNull(result.getIV()),
                () -> assertEquals(EncryptionMethod.A256GCM, result.getHeader().getEncryptionMethod())
        );
    }

    @Test
    void given_defaultData_when_writePidAsClaims_then_ok() {
        var pid = PidCredentialData.Companion.getTEST_DATA_SET();

        var result = out.writePidAsClaims(pid);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(pid.getGivenName(), result.getStringClaim(SeedPidService.GIVEN_NAME))
        );

        if (log.isDebugEnabled()) {
            log.debug("claims: {}", result);
        }
    }

    @Test
    void given_defaultData_when_writePidAsClaims_and_readPidFromClaims_then_equal() {
        var pid = PidCredentialData.Companion.getTEST_DATA_SET();

        var claims = out.writePidAsClaims(pid);
        var result = out.readPidFromClaims(claims);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(pid, result)
        );
    }

    @Test
    void given_malformedUserBindingKey_when_readFromClaims_Enc_then_fail() {
        // missing cnf
        var claims = given_claimsForIssuerAndExpiration(ISSUER_ID, Instant.now().plusSeconds(300))
                .build();
        var exception = assertThrows(SeedException.class, () -> out.readFromClaimsEnc(claims, ISSUER_ID));
        assertEquals(SeedException.Kind.MISSING_DATA, exception.getKind());

        // cnf has invalid value type
        var claims2 = given_claimsForIssuerAndExpiration(ISSUER_ID, Instant.now().plusSeconds(300))
                .claim(SeedPidService.CONFIRMATION, 123)
                .build();
        exception = assertThrows(SeedException.class, () -> out.readFromClaimsEnc(claims2, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());

        // missing cnf.key
        var empty = JSONObjectUtils.newJSONObject();
        var claims3 = given_claimsForIssuerAndExpiration(ISSUER_ID, Instant.now().plusSeconds(300))
                .claim(SeedPidService.CONFIRMATION, empty)
                .build();
        exception = assertThrows(SeedException.class, () -> out.readFromClaimsEnc(claims3, ISSUER_ID));
        assertEquals(SeedException.Kind.MISSING_DATA, exception.getKind());

        // cnf.key has invalid type
        var unexpected = JSONObjectUtils.newJSONObject();
        unexpected.put(SeedPidService.KEY, 1234);
        var claims4 = given_claimsForIssuerAndExpiration(ISSUER_ID, Instant.now().plusSeconds(300))
                .claim(SeedPidService.CONFIRMATION, unexpected)
                .build();
        exception = assertThrows(SeedException.class, () -> out.readFromClaimsEnc(claims4, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());

        // cnf.key is not a Key
        var cnf = JSONObjectUtils.newJSONObject();
        var bad = JSONObjectUtils.newJSONObject();
        cnf.put(SeedPidService.KEY, bad);
        bad.put("x", 1234);
        var claims5 = given_claimsForIssuerAndExpiration(ISSUER_ID, Instant.now().plusSeconds(300))
                .claim(SeedPidService.CONFIRMATION, cnf)
                .build();
        exception = assertThrows(SeedException.class, () -> out.readFromClaimsEnc(claims5, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());
    }

    @Test
    void given_differentIssuer_when_readFromClaims_Enc_then_fail() {
        var claims = given_claimsForIssuerAndExpiration(ISSUER_ID + "foo", Instant.now().plusSeconds(300))
                .build();

        var exception = assertThrows(SeedException.class, () -> out.readFromClaimsEnc(claims, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());
    }

    @Test
    void given_expiredJwt_when_readFromClaims_Enc_then_fail() {
        var claims = given_claimsForIssuerAndExpiration(ISSUER_ID, Instant.now().minusSeconds(30))
                .build();

        var exception = assertThrows(SeedException.class, () -> out.readFromClaimsEnc(claims, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());
    }

    @Test
    void given_defaultData_when_readFromClaims_Enc_then_ok() throws ParseException, JOSEException {
        var s = given_defaultDataClaimsEnc();
        var claims = JWTClaimsSet.parse(s);
        Mockito.when(trustManager.encryptionKeyForKeyId(ENCRYPT_KEY_ID)).thenReturn(secretKey);

        var result = out.readFromClaimsEnc(claims, ISSUER_ID);
        assertNotNull(result);
        assertEquals("ERIKA", result.pidCredentialData().getGivenName());
    }

    @Test
    void given_malformedUserBindingKey_when_readFromClaims_Pin_then_fail() {
        // missing cnf
        var claims = given_claimsForIssuerAndExpiration(ISSUER_ID, Instant.now().plusSeconds(300))
                .build();
        var exception = assertThrows(SeedException.class, () -> out.readFromClaimsPin(claims, ISSUER_ID));
        assertEquals(SeedException.Kind.MISSING_DATA, exception.getKind());

        // cnf has invalid value type
        var claims2 = given_claimsForIssuerAndExpiration(ISSUER_ID, Instant.now().plusSeconds(300))
                .claim(SeedPidService.CONFIRMATION, 123)
                .build();
        exception = assertThrows(SeedException.class, () -> out.readFromClaimsPin(claims2, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());

        // missing cnf.key
        var empty = JSONObjectUtils.newJSONObject();
        var claims3 = given_claimsForIssuerAndExpiration(ISSUER_ID, Instant.now().plusSeconds(300))
                .claim(SeedPidService.CONFIRMATION, empty)
                .build();
        exception = assertThrows(SeedException.class, () -> out.readFromClaimsPin(claims3, ISSUER_ID));
        assertEquals(SeedException.Kind.MISSING_DATA, exception.getKind());

        // cnf.key has invalid type
        var unexpected = JSONObjectUtils.newJSONObject();
        unexpected.put(SeedPidService.KEY, 1234);
        var claims4 = given_claimsForIssuerAndExpiration(ISSUER_ID, Instant.now().plusSeconds(300))
                .claim(SeedPidService.CONFIRMATION, unexpected)
                .build();
        exception = assertThrows(SeedException.class, () -> out.readFromClaimsPin(claims4, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());

        // cnf.key is not a Key
        var cnf = JSONObjectUtils.newJSONObject();
        var bad = JSONObjectUtils.newJSONObject();
        cnf.put(SeedPidService.KEY, bad);
        bad.put("x", 1234);
        var claims5 = given_claimsForIssuerAndExpiration(ISSUER_ID, Instant.now().plusSeconds(300))
                .claim(SeedPidService.CONFIRMATION, cnf)
                .build();
        exception = assertThrows(SeedException.class, () -> out.readFromClaimsPin(claims5, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());

        // TODO test cnf.pin_derived_public_jwk
    }

    @Test
    void given_differentIssuer_when_readFromClaims_Pin_then_fail() {
        var claims = given_claimsForIssuerAndExpiration(ISSUER_ID + "foo", Instant.now().plusSeconds(300))
                .build();

        var exception = assertThrows(SeedException.class, () -> out.readFromClaimsPin(claims, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());
    }

    @Test
    void given_expiredJwt_when_readFromClaims_Pin_then_fail() {
        var claims = given_claimsForIssuerAndExpiration(ISSUER_ID, Instant.now().minusSeconds(30))
                .build();

        var exception = assertThrows(SeedException.class, () -> out.readFromClaimsPin(claims, ISSUER_ID));
        assertEquals(SeedException.Kind.INVALID, exception.getKind());
    }

    @Test
    void given_defaultData_when_readFromClaims_Pin_then_ok() throws ParseException {
        var s = given_defaultDataClaimsPin();
        var claims = JWTClaimsSet.parse(s);

        var result = out.readFromClaimsPin(claims, ISSUER_ID);
        assertNotNull(result);
        assertEquals("ERIKA", result.pidCredentialData().getGivenName());
    }

    String given_defaultDataClaimsEnc() throws ParseException, JOSEException {
        Instant now = Instant.now();
        Instant expires = now.plus(30, ChronoUnit.DAYS);
        var cnf = JSONObjectUtils.newJSONObject();
        var key = given_userBindingKey().toJSONObject();
        cnf.put(SeedPidService.KEY, key);
        var builder = new JWTClaimsSet.Builder()
                .issuer(ISSUER_ID)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expires))
                .claim(SeedPidService.CONFIRMATION, cnf);
        var claims = out.writePidAsClaims(PidCredentialData.Companion.getTEST_DATA_SET());
        JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                .keyID(ENCRYPT_KEY_ID)
                .build();
        var jwt = new EncryptedJWT(header, claims);
        jwt.encrypt(new DirectEncrypter(secretKey));
        var jwe = jwt.serialize();
        builder.claim(SeedPidService.DATA_ENC, jwe);
        return builder.build().toString();
    }

    String given_defaultDataClaimsPin() throws ParseException {
        Instant now = Instant.now();
        Instant expires = now.plus(30, ChronoUnit.DAYS);
        var cnf = JSONObjectUtils.newJSONObject();
        var key = given_userBindingKey().toJSONObject();
        var pinDerivedKey = given_pinDerivedPublicKey().toJSONObject();
        cnf.put(SeedPidService.KEY, key);
        cnf.put(SeedPidService.PIN_DERIVED_KEY, pinDerivedKey);
        var builder = new JWTClaimsSet.Builder()
                .issuer(ISSUER_ID)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expires))
                .claim(SeedPidService.CONFIRMATION, cnf);
        var claims = out.writePidAsClaims(PidCredentialData.Companion.getTEST_DATA_SET());
        builder.claim(SeedPidService.DATA_JSON, claims.toJSONObject());
        return builder.build().toString();
    }

    private static JWTClaimsSet.Builder given_claimsForIssuerAndExpiration(String issuer, Instant expiresAt) {
        Instant issued = Instant.now().minusSeconds(120);
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .issueTime(Date.from(issued))
                .expirationTime(Date.from(expiresAt));
    }

    JWK given_userBindingKey() throws ParseException {
        return publicJwk();
    }

    JWK given_clientInstanceKey() throws ParseException {
        return publicJwk();
    }

    JWK given_pinDerivedPublicKey() throws ParseException {
        return publicJwk();
    }

    private ECKey publicJwk() throws ParseException {
        ECKey key = ECKey.parse(USER_KEY_STRING);
        if (log.isDebugEnabled()) {
            log.debug("ECKey: {}", key.toJSONString());
        }
        return key.toPublicJWK();
    }

    private void given_seedEncrypter() {
        var seedEncrypter = new SeedEncrypter(ENCRYPT_KEY_ID, secretKey);
        Mockito.when(trustManager.currentEncrypter()).thenReturn(seedEncrypter);
    }

}
