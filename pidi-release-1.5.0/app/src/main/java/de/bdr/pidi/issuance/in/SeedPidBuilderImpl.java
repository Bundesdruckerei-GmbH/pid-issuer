/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.in;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.SeedException;
import de.bdr.pidi.authorization.out.issuance.SeedPidBuilder;
import de.bdr.pidi.issuance.core.service.SeedPidService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Set;

@Service
public class SeedPidBuilderImpl implements SeedPidBuilder {

    private static final Set<String> HEADER_PARAMETERS = Set.of("alg", "kid");

    private final SeedPidService service;

    protected SeedPidBuilderImpl(SeedPidService service) {
        this.service = service;
    }

    @Override
    public String build(PidCredentialData pidCredentialData, JWK devicePublicKey, String issuerId)
            throws SeedException {
        ensurePresent(pidCredentialData, "pidCredentialData");
        ensurePresent(devicePublicKey, "devicePublicKey");
        ensurePresent(issuerId, "issuerId");

        var claims = service.writeAsClaims(pidCredentialData, devicePublicKey, issuerId);
        return build(claims).serialize();
    }

    @Override
    public String build(PidCredentialData pidCredentialData, JWK clientInstanceKey, JWK pinDerivedPublicKey, String issuerId) throws SeedException {
        ensurePresent(pidCredentialData, "pidCredentialData");
        ensurePresent(clientInstanceKey, "clientInstanceKey");
        ensurePresent(pinDerivedPublicKey, "pinDerivedPublicKey");
        ensurePresent(issuerId, "issuerId");

        var claims = service.writeAsClaims(pidCredentialData, clientInstanceKey, pinDerivedPublicKey, issuerId);
        return build(claims).serialize();
    }

    private SignedJWT build(JWTClaimsSet claims) throws SeedException {
        var seedSigner = service.currentSigner();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(seedSigner.keyIdentifier())
                .build();
        var jws = new SignedJWT(header, claims);
        try {
            jws.sign(seedSigner.signer());
        } catch (JOSEException e) {
            throw new SeedException(SeedException.Kind.CRYPTO, "could not sign the seed PID", e);
        }
        return jws;
    }

    @Override
    public SeedPidBuilder.SeedData extractVerifiedEncSeed(String seedPid, String issuerId) {
        ensurePresent(seedPid, "seedPid");
        ensurePresent(issuerId, "issuerId");

        try {
            var jws = SignedJWT.parse(seedPid);
            verifySignature(jws);
            var claims = jws.getJWTClaimsSet();
            return service.readFromClaimsEnc(claims, issuerId);
        } catch (ParseException pe) {
            throw new SeedException(SeedException.Kind.INVALID, "seed PID could not be parsed", pe);
        }
    }

    // TODO PIDI-2266 refactor to remove duplicate code; maybe abstract class?
    @Override
    public SeedPidBuilder.SeedData extractVerifiedPinSeed(String seedPid, String issuerId) {
        ensurePresent(seedPid, "seedPid");
        ensurePresent(issuerId, "issuerId");

        try {
            var jws = SignedJWT.parse(seedPid);
            verifySignature(jws);
            var claims = jws.getJWTClaimsSet();
            return service.readFromClaimsPin(claims, issuerId);
        } catch (ParseException pe) {
            throw new SeedException(SeedException.Kind.INVALID, "seed PID could not be parsed", pe);
        }
    }

    protected void verifySignature(SignedJWT jws) {
        JWSHeader header = jws.getHeader();
        if (!HEADER_PARAMETERS.equals(header.getIncludedParams())) {
            throw new SeedException(SeedException.Kind.INVALID, "headers do not match expectations");
        }
        if (!JWSAlgorithm.ES256.equals(header.getAlgorithm())) {
            throw new SeedException(SeedException.Kind.CRYPTO, "unexpected signature algorithm in seedPid");
        }
        var keyId = header.getKeyID();
        ensurePresent(keyId, "header.kid");
        var verifier = service.verifierForKeyId(keyId);
        try {
            if (!jws.verify(verifier)) {
                throw new SeedException(SeedException.Kind.INVALID, "seed signature is not valid");
            }
        } catch (JOSEException e) {
            throw new SeedException(SeedException.Kind.CRYPTO, "could not verify seed signature", e);
        }
    }

    private static void ensurePresent(String parameter, String name) {
        if (StringUtils.isBlank(parameter)) {
            throw new SeedException(SeedException.Kind.INVALID, "no %s given".formatted(name));
        }
    }

    private static void ensurePresent(Object parameter, String name) {
        if (parameter == null) {
            throw new SeedException(SeedException.Kind.INVALID, "no %s given".formatted(name));
        }
    }

}
