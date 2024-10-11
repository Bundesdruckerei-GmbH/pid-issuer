/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.walletattestation.core;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import de.bdr.openid4vc.vci.service.attestation.ClientAttestationCnf;
import de.bdr.pidi.base.IssuedAtValidationResult;
import de.bdr.pidi.base.IssuedAtValidator;
import de.bdr.pidi.clientconfiguration.ClientConfigurationService;
import de.bdr.pidi.walletattestation.ClientAttestationJwt;
import de.bdr.pidi.walletattestation.ClientAttestationPopJwt;
import de.bdr.pidi.walletattestation.WalletAttestationRequest;
import de.bdr.pidi.walletattestation.WalletAttestationService;
import de.bdr.pidi.walletattestation.config.AttestationConfiguration;
import kotlin.NotImplementedError;
import lombok.extern.slf4j.Slf4j;
import org.jmolecules.architecture.hexagonal.Application;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@Application
public class WalletAttestation implements WalletAttestationService {
    private final ClientConfigurationService clientConfigurationService;

    private final Duration proofTimeTolerance;
    private final Duration proofValidity;

    public WalletAttestation(AttestationConfiguration configuration, ClientConfigurationService clientConfigurationService) {
        this.proofTimeTolerance = configuration.getProofTimeTolerance();
        this.proofValidity = configuration.getProofValidity();
        this.clientConfigurationService = clientConfigurationService;
    }

    @Override
    public boolean isValidWallet(WalletAttestationRequest request) {
        validateClientAttestationJwt(request.clientAttestationJwt(), request.clientId());
        validateClientAttestationPopJwt(request.clientAttestationPopJwt(), request.clientId(), request.clientAttestationJwt().cnf(), request.issuerIdentifier());
        log.info("Validating attestation to be true");
        return true;
    }

    private void validateClientAttestationJwt(ClientAttestationJwt jwt, String clientId) {
        validateTimeClaims(jwt);
        if (!clientConfigurationService.isValidClientId(UUID.fromString(jwt.iss()))) {
            throw new IllegalArgumentException("Client attestation issuer '" + jwt.iss() + "' is not supported");
        }
        if (!jwt.sub().equalsIgnoreCase(clientId)) {
            throw new IllegalArgumentException("Client attestation subject '" + jwt.sub() + "' does not match the client id '" + clientId + "'");
        }
        try {
            var jwk = clientConfigurationService.getJwk(UUID.fromString(clientId));
            var verifier = jwsVerifierForKey(jwk);
            if (!jwt.signedJWT().verify(verifier)) {
                throw new IllegalArgumentException("Client attestation signature verification failed");
            }
        } catch (JOSEException | IllegalStateException | NotImplementedError e) {
            throw new IllegalArgumentException("An error occurred while checking the signature in client attestation jwt");
        }
    }

    private void validateClientAttestationPopJwt(ClientAttestationPopJwt jwt, String clientId, ClientAttestationCnf cnf, String issuerIdentifier) {
        validateTimeClaims(jwt);
        if (!jwt.iss().equalsIgnoreCase(clientId)) {
            throw new IllegalArgumentException("Client attestation issuer '" + jwt.iss() + "' does not match the client id '" + clientId + "'");
        }
        if (!jwt.aud().contains(issuerIdentifier)) {
            log.debug("looking for audience {}", issuerIdentifier);
            log.debug("audiences present in request: {}", jwt.aud());
            throw new IllegalArgumentException("Client attestation issuer audience unknown");
        }
        try {
            var verifier = jwsVerifierForKey(cnf.getJwk());
            if (!jwt.signedJWT().verify(verifier)) {
                throw new IllegalArgumentException("Client attestation signature verification failed");
            }
        } catch (JOSEException | IllegalStateException | NotImplementedError e) {
            throw new IllegalArgumentException("An error occurred while checking the signature in client attestation pop jwt");
        }
    }

    private void validateTimeClaims(ClientAttestationJwtBase jwt) {
        var now = Instant.now();
        if (jwt.exp().plus(proofTimeTolerance).isBefore(now)) {
            throw new IllegalArgumentException("Client attestation is expired");
        }
        if (jwt.exp().isAfter(now.plus(proofValidity).plus(proofTimeTolerance))) {
            throw new IllegalArgumentException("Client attestation expiration date is too far in the future");
        }
        if (jwt.nbf() != null && now.plus(proofTimeTolerance).isBefore(jwt.nbf())) {
            throw new IllegalArgumentException("Client attestation is not yet valid");
        }
        IssuedAtValidationResult issuedAtValidationResult = IssuedAtValidator.validate(jwt.iat(), now, proofTimeTolerance, proofValidity);
        if (issuedAtValidationResult == IssuedAtValidationResult.IN_FUTURE) {
            throw new IllegalArgumentException("Client attestation is issued in the future");
        } else if (issuedAtValidationResult == IssuedAtValidationResult.TOO_OLD) {
            throw new IllegalArgumentException("Client attestation issuance is too old");
        }
    }

    public JWSVerifier jwsVerifierForKey(JWK jwk) throws JOSEException {
        return switch (jwk.getKeyType().getValue()) {
            case "OKP" -> new Ed25519Verifier(jwk.toOctetKeyPair());
            case "RSA" -> new RSASSAVerifier(jwk.toRSAKey());
            case "EC" -> new ECDSAVerifier(jwk.toECKey());
            case null -> throw new IllegalArgumentException("Client attestation signature JWS type not supported");
            default -> throw new NotImplementedError("JWK signing algorithm not implemented");
        };
    }
}
