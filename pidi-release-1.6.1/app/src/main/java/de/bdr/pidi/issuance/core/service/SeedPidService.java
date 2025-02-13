/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.core.service;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import de.bdr.pidi.authorization.out.identification.Address;
import de.bdr.pidi.authorization.out.identification.BirthPlace;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.SeedException;
import de.bdr.pidi.authorization.out.issuance.SeedPidBuilder;
import org.apache.commons.lang3.ObjectUtils;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class SeedPidService {
    public static final String ADDRESS = "address";
    public static final String BIRTHDATE = "birthdate";
    public static final String BIRTH_FAMILY_NAME = "birth_family_name";
    public static final String CONFIRMATION = "cnf";
    public static final String COUNTRY = "country";
    public static final String DATA_ENC = "pid_data_enc";
    public static final String DATA_JSON = "pid_data";
    public static final String EXPIRATION = "exp";
    public static final String PSEUDONYM = "pseudonym";
    public static final String FAMILY_NAME = "family_name";
    public static final String FORMATTED = "formatted";
    public static final String GIVEN_NAME = "given_name";
    public static final String ISSUED_AT = "iat";
    public static final String ISSUER = "iss";
    public static final String KEY = "jwk";
    public static final String PIN_DERIVED_KEY = "pin_derived_public_jwk";
    public static final String LOCALITY = "locality";
    public static final String NATIONALITY = "nationality";
    public static final String PLACE_OF_BIRTH = "place_of_birth";
    public static final String POSTAL_CODE = "postal_code";
    public static final String REGION = "region";
    public static final String STREET = "street_address";
    // list of registered claims
    // https://www.iana.org/assignments/jwt/jwt.xhtml

    // cnf in Proof-of-Possession
    // https://www.rfc-editor.org/rfc/rfc7800.html

    private final DateTimeFormatter birthdateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SeedTrustManager trustManager;
    private final Duration seedValidity;

    public SeedPidService(SeedTrustManager trustManager, Duration seedValidity) {
        this.trustManager = trustManager;
        this.seedValidity = seedValidity;
    }

    public SeedSigner currentSigner() {
        return this.trustManager.currentSigner();
    }

    public JWSVerifier verifierForKeyId(String keyId) {
        return this.trustManager.verifierForKeyId(keyId);
    }

    public JWTClaimsSet writeAsClaims(PidCredentialData pidCredentialData, JWK devicePublicKey, String issuerId) {
        var cb = initClaimsBuilder(issuerId);

        // cb.claim("auth_time", authentication time, OpenID Connect Core 1.0 Section 2

        // compact serialization  https://datatracker.ietf.org/doc/html/rfc7516#section-7.1
        // including the iv and an authentication tag
        cb.claim(DATA_ENC, writePidAsJwe(pidCredentialData).serialize());
        var cnf = JSONObjectUtils.newJSONObject();
        cnf.put(KEY, devicePublicKey.toJSONObject());
        cb.claim(CONFIRMATION, cnf);

        return cb.build();
    }

    public JWTClaimsSet writeAsClaims(PidCredentialData pidCredentialData, JWK clientInstanceKey, JWK pinDerivedPublicKey, String issuerId) {
        var cb = initClaimsBuilder(issuerId);

        cb.claim(DATA_JSON, writePidAsClaims(pidCredentialData).toJSONObject());
        var cnf = JSONObjectUtils.newJSONObject();
        cnf.put(KEY, clientInstanceKey.toJSONObject());
        cnf.put(PIN_DERIVED_KEY, pinDerivedPublicKey.toJSONObject());
        cb.claim(CONFIRMATION, cnf);

        return cb.build();
    }

    private JWTClaimsSet.Builder initClaimsBuilder(String issuerId) {
        var cb = new JWTClaimsSet.Builder();
        cb.issuer(issuerId);

        Instant now = Instant.now();
        cb.issueTime(new Date(now.toEpochMilli()));
        Instant end = now.plus(seedValidity);
        cb.expirationTime(new Date(end.toEpochMilli()));
        return cb;
    }

    public SeedPidBuilder.SeedData readFromClaimsEnc(JWTClaimsSet claims, String expectedIssuerId) {
        var builder = new SeedDataBuilder();
        setSharedClaims(claims, expectedIssuerId, builder);
        try {
            builder.setHolderBindingKey(getConfirmationJWK(claims, KEY));
            String dataJwe = read(claims, DATA_ENC);
            var jwe = EncryptedJWT.parse(dataJwe);
            builder.setPidCredentialData(readPidFromJwe(jwe));
        } catch (ParseException e) {
            throw new SeedException(SeedException.Kind.INVALID, "could not decode payload data", e);
        }
        return builder.build();
    }

    // TODO PIDI-2266 refactor to remove duplicate code, maybe abstract class?

    public SeedPidBuilder.SeedData readFromClaimsPin(JWTClaimsSet claims, String expectedIssuerId) {
        var builder = new SeedDataBuilder();
        setSharedClaims(claims, expectedIssuerId, builder);
        try {
            builder.setClientInstanceKey(getConfirmationJWK(claims, KEY));
            builder.setPinDerivedKey(getConfirmationJWK(claims, PIN_DERIVED_KEY));
            var dataMap = readMap(claims, DATA_JSON);
            var dataClaims = JWTClaimsSet.parse(dataMap);
            builder.setPidCredentialData(readPidFromClaims(dataClaims));
        } catch (ParseException e) {
            throw new SeedException(SeedException.Kind.INVALID, "could not decode payload data", e);
        }
        return builder.build();
    }

    private void setSharedClaims(JWTClaimsSet claims, String expectedIssuerId, SeedDataBuilder builder) {
        String issuerId = read(claims, ISSUER);
        if (!issuerId.equals(expectedIssuerId)) {
            throw new SeedException(SeedException.Kind.INVALID, "the issuer does not match");
        }
        Instant issuedAt = readInstant(claims, ISSUED_AT);
        Instant expires = readInstant(claims, EXPIRATION);
        if (expires.isBefore(Instant.now())) {
            throw new SeedException(SeedException.Kind.INVALID, "seed PID is expired");
        }
        builder.setIssuerId(issuerId).setIssuedAt(issuedAt).setExpiresAt(expires);
    }

    private static JWK getConfirmationJWK(JWTClaimsSet claims, String name) throws ParseException {
        var cnf = claims.getJSONObjectClaim(CONFIRMATION);
        if (cnf == null) {
            throw missingClaim(CONFIRMATION);
        }
        var key = JSONObjectUtils.getJSONObject(cnf, name);
        if (key == null) {
            throw missingClaim(CONFIRMATION + "." + name);
        }
        return JWK.parse(key);
    }

    EncryptedJWT writePidAsJwe(PidCredentialData pidCredentialData) {
        var claims = writePidAsClaims(pidCredentialData);

        var seedEncrypter = this.trustManager.currentEncrypter();
        JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                .keyID(seedEncrypter.keyIdentifier())
                .build();
        var jwt = new EncryptedJWT(header, claims);
        try {
            jwt.encrypt(new DirectEncrypter(seedEncrypter.key()));
            return jwt;
        } catch (JOSEException e) {
            throw new SeedException(SeedException.Kind.CRYPTO, "could not encrypt the data", e);
        }
    }

    PidCredentialData readPidFromJwe(EncryptedJWT jwe)  {
        JWEHeader header = jwe.getHeader();
        if (! JWEAlgorithm.DIR.equals(header.getAlgorithm())) {
            throw new SeedException(SeedException.Kind.INVALID, "unexpected encryption algorithm");
        }
        if (! EncryptionMethod.A256GCM.equals(header.getEncryptionMethod())) {
            throw new SeedException(SeedException.Kind.INVALID, "unexpected encryption method");
        }
        var keyId = header.getKeyID();
        if (keyId == null) {
            throw new SeedException(SeedException.Kind.MISSING_DATA, "JWE keyId missing");
        }
        var key = this.trustManager.encryptionKeyForKeyId(keyId);

        try {
            jwe.decrypt(new DirectDecrypter(key));
        } catch (JOSEException e) {
            throw new SeedException(SeedException.Kind.CRYPTO, "could not decrypt the data", e);
        }
        JWTClaimsSet claims;
        try {
            claims = jwe.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new SeedException(SeedException.Kind.INVALID, "could not decode payload", e);
        }
        if (claims == null) {
            throw new SeedException(SeedException.Kind.INVALID, "encrypted payload has no claims");
        }
        return readPidFromClaims(claims);
    }

    JWTClaimsSet writePidAsClaims(PidCredentialData pidCredentialData) {
        JWTClaimsSet.Builder cb = new JWTClaimsSet.Builder();
        cb.claim(PSEUDONYM, pidCredentialData.getPseudonym());
        cb.claim(FAMILY_NAME, pidCredentialData.getFamilyName());
        cb.claim(GIVEN_NAME, pidCredentialData.getGivenName());

        cb.claim(BIRTHDATE, birthdateFormatter.format(pidCredentialData.getBirthdate()));
        write(pidCredentialData.getBirthFamilyName(), cb, BIRTH_FAMILY_NAME);
        var bp = pidCredentialData.getPlaceOfBirth();
        if (bp != null) {
            var place = JSONObjectUtils.newJSONObject();
            write(bp.getLocality(), place, LOCALITY);
            write(bp.getRegion(), place, REGION);
            write(bp.getCountry(), place, COUNTRY);
            cb.claim(PLACE_OF_BIRTH, place);
        }
        write(pidCredentialData.getNationality(), cb, NATIONALITY);
        var addr = pidCredentialData.getAddress();
        if (addr != null) {
            var ao = JSONObjectUtils.newJSONObject();
            write(addr.getFormatted(), ao, FORMATTED);
            write(addr.getStreetAddress(), ao, STREET);
            write(addr.getLocality(), ao, LOCALITY);
            write(addr.getCountry(), ao, COUNTRY);
            write(addr.getPostalCode(), ao, POSTAL_CODE);
            write(addr.getRegion(), ao, REGION);
            cb.claim(ADDRESS, ao);
        }
        return cb.build();
    }

    PidCredentialData readPidFromClaims(JWTClaimsSet claims) {
        try {
            String pseudonym = read(claims, PSEUDONYM);
            String familyName = read(claims, FAMILY_NAME);
            String givenName = read(claims, GIVEN_NAME);
            LocalDate birthdate = LocalDate.from(birthdateFormatter.parse(read(claims, BIRTHDATE)));
            BirthPlace birthPlace = readBirthPlace(claims);
            String birthFamilyName = readNullable(claims, BIRTH_FAMILY_NAME);
            Address address = readAddress(claims);
            String nationality = readNullable(claims, NATIONALITY);
            return new PidCredentialData(pseudonym, familyName, givenName, birthdate, birthPlace, birthFamilyName, address, nationality);
        } catch (ParseException e) {
            throw new SeedException(SeedException.Kind.INVALID, "could not decode the seed data", e);
        }
    }

    private BirthPlace readBirthPlace(JWTClaimsSet claims) throws ParseException {
        BirthPlace result = null;
        Map<String, Object> json = claims.getJSONObjectClaim(PLACE_OF_BIRTH);
        if (json != null) {
            try {
                var locality = JSONObjectUtils.getString(json, LOCALITY);
                var country = JSONObjectUtils.getString(json, COUNTRY);
                var region = JSONObjectUtils.getString(json, REGION);
                if (! ObjectUtils.allNull(locality, country, region)) {
                    result = new BirthPlace(locality, country, region);
                }
            } catch (ParseException e) {
                throw new SeedException(SeedException.Kind.INVALID, "could not decode birth place", e);
            }
        }
        return result;
    }

    private Address readAddress(JWTClaimsSet claims) throws ParseException {
        Address result = null;
        Map<String, Object> json = claims.getJSONObjectClaim(ADDRESS);
        if (json != null) {
            try {
                var formatted = JSONObjectUtils.getString(json, FORMATTED);
                var country = JSONObjectUtils.getString(json, COUNTRY);
                var region = JSONObjectUtils.getString(json, REGION);
                var locality = JSONObjectUtils.getString(json, LOCALITY);
                var postalCode = JSONObjectUtils.getString(json, POSTAL_CODE);
                var street = JSONObjectUtils.getString(json, STREET);
                result = new Address(formatted, country, region, locality, postalCode, street);
            } catch (ParseException e) {
                throw new SeedException(SeedException.Kind.INVALID, "could not decode address", e);
            }
        }
        return result;
    }

    private static void write(String value, Map<String, Object> json, String name) {
        if (value != null) {
            json.put(name, value);
        }
    }
    private static void write(String value, JWTClaimsSet.Builder builder, String name) {
        if (value != null) {
            builder.claim(name, value);
        }
    }

    private static String readNullable(JWTClaimsSet claims, String name) {
        Object o = claims.getClaim(name);
        return switch (o) {
            case String s -> s;
            case null -> null;
            default -> throw wrongTypeClaim(name);
        };
    }

    private static String read(JWTClaimsSet claims, String name) {
        Object o = claims.getClaim(name);
        return switch (o) {
            case String s -> s;
            case null -> throw missingClaim(name);
            default -> throw wrongTypeClaim(name);
        };
    }

    private static Map<String, Object> readMap(JWTClaimsSet claims, String name) {
        try {
            var o = claims.getJSONObjectClaim(name);
            return switch (o) {
                case Map<String, Object> m -> m;
                case null -> throw missingClaim(name);
            };
        } catch (ParseException e) {
            throw wrongTypeClaim(name);
        }
    }

    private static SeedException wrongTypeClaim(String name) {
        return new SeedException(SeedException.Kind.INVALID, "wrong type of claim %s".formatted(name));
    }

    private static SeedException missingClaim(String name) {
        return new SeedException(SeedException.Kind.MISSING_DATA, "missing claim %s".formatted(name));
    }

    private static Instant readInstant(JWTClaimsSet claims, String name) {
        Object o = claims.getClaim(name);
        var timeClaims = Set.of(ISSUED_AT, EXPIRATION);

        return switch (o) {
            case Date d when timeClaims.contains(name) -> d.toInstant();
            case Number n when !timeClaims.contains(name) -> Instant.ofEpochSecond(n.longValue());
            case null -> throw missingClaim(name);
            default -> throw wrongTypeClaim(name);
        };
    }
}
