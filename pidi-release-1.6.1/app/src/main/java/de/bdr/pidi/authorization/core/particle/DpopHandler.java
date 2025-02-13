/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.particle;

import com.nimbusds.jose.jwk.JWK;
import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.data.storage.JtiStorage;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.openid4vc.vci.service.dpop.DPoPProof;
import de.bdr.openid4vc.vci.service.dpop.DPoPValidator;
import de.bdr.openid4vc.vci.service.endpoints.MissingNonceException;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.Nonce;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidGrantException;
import de.bdr.pidi.authorization.core.exception.SessionNotFoundException;
import de.bdr.pidi.authorization.core.exception.UnauthorizedException;
import de.bdr.pidi.authorization.core.service.NonceService;
import de.bdr.pidi.base.requests.SeedCredentialRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
public class DpopHandler implements OidHandler {

    public static final String DPOP_NONCE_HEADER = "DPoP-Nonce";
    /*
    * Dummy JtiStorage, which always returns true on method  isUnused(jti, validUntil)
     */
    private final JtiStorage jtiStorage = (s, instant) -> true;

    private final NonceService nonceService;
    private final Duration proofTimeTolerance;
    private final Duration proofValidity;
    private final URL baseUrl; // is used in the openid4vc lib, only to extract the scheme
    private final String scheme;
    private final boolean compareJwkWithClientInstanceKey;

    @Override
    public void processFinishAuthRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        generateAndIssueDpopNonce(response, session);
    }

    @Override
    public void processTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        try {
            handleTokenValidationAndIssuance(request, response, session);
        } catch (InvalidDpopProofException e) {
            Nonce nonce = nonceService.generateAndStoreDpopNonce(session);
            e.addHeader(DPOP_NONCE_HEADER, nonce.nonce());
            throw e;
        }
    }

    @Override
    public void processSeedCredentialTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        handleTokenValidationAndIssuance(request, response, session);
    }

    @Override
    public void processRefreshTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        handleTokenValidationAndIssuance(request, response, session);
    }

    @Override
    public void processCredentialRequest(HttpRequest<CredentialRequest> request, WResponseBuilder response, WSession session) {
        handleCredentialRequest(request, session);
    }

    @Override
    public void processSeedCredentialRequest(HttpRequest<SeedCredentialRequest> request, WResponseBuilder response, WSession session) {
        handleCredentialRequest(request, session);
    }

    private void handleTokenValidationAndIssuance(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        validateAndSaveDpopProof(request, session);
        generateAndIssueDpopNonce(response, session);
    }

    private void handleCredentialRequest(HttpRequest<?> request, WSession session) {
        final JWK dpopPublicKey = session.getCheckedParameterAsJwk(SessionKey.DPOP_PUBLIC_KEY);
        try {
            validateDpopProof(request, session, dpopPublicKey);
        } catch (UseDpopNonceException udne) {
            throw new UnauthorizedException(scheme, udne.getErrorCode(), udne);
        } catch (InvalidDpopProofException e) {
            Nonce nonce = nonceService.generateAndStoreDpopNonce(session);
            e.addHeader(DPOP_NONCE_HEADER, nonce.nonce());
            throw new UnauthorizedException(scheme, e.getErrorCode(), e);
        }
    }

    private void validateAndSaveDpopProof(HttpRequest<?> request, WSession session) {
        var dpop = validateDpopProof(request, session);
        validateJwkKnown(dpop, session);
        session.putParameter(SessionKey.DPOP_PUBLIC_KEY, dpop.getJwk().toJSONString());
        session.putParameter(SessionKey.DPOP_IDENTIFIER, dpop.getJti());
    }

    private DPoPProof validateDpopProof(HttpRequest<?> request, WSession session) {
        return validateDpopProof(request, session, null);
    }

    private DPoPProof validateDpopProof(HttpRequest<?> request, WSession session, JWK accessTokenJwk) {
        var nonceValidator = new DpopNonceValidator(session, nonceService);
        try {
            var validator = new DPoPValidator(baseUrl.toString(), jtiStorage, nonceValidator, proofValidity, proofTimeTolerance);
            var proof = validator.validate(request, accessTokenJwk);
            if (proof == null) {
                throw new InvalidDpopProofException("DPoP header not present");
            }
            return proof;
        } catch (MissingNonceException e) {
            throw new UseDpopNonceException(e.getNonce(), e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new InvalidDpopProofException(e.getMessage());
        }
    }

    private void generateAndIssueDpopNonce(WResponseBuilder response, WSession session) {
        var nonce = nonceService.generateAndStoreDpopNonce(session);

        response.addStringHeader(DPOP_NONCE_HEADER, nonce.nonce());
    }

    private void validateJwkKnown(DPoPProof dpop, WSession session) {
        if (compareJwkWithClientInstanceKey) {
            var clientInstanceKey = session.getCheckedParameterAsJwk(SessionKey.CLIENT_INSTANCE_KEY);
            if (!dpop.getJwk().equals(clientInstanceKey)) {
                throw new InvalidGrantException("Key mismatch");
            }
        }
    }

    private record DpopNonceValidator(WSession session, NonceService nonceService)
            implements de.bdr.openid4vc.common.vci.NonceService {

        @NotNull
        @Override
        public NonceAndValidityDuration generate(@NotNull NoncePurpose purpose) {
            Nonce nonce = nonceService.generateAndStoreDpopNonce(session);
            return new NonceAndValidityDuration(nonce.nonce(), nonce.expiresIn());
        }

        @Override
        public void validate(@NotNull String s, @NotNull NoncePurpose purpose) {
            try {
                var nonce = nonceService.fetchDpopNonceFromSession(session);
                if (!s.equals(nonce.nonce())) {
                    throw new UseDpopNonceException(generate(purpose).getNonce(), "DPoP nonce is invalid");
                }
                if (Instant.now().isAfter(nonce.expirationTime())) {
                    throw new InvalidDpopProofException("DPoP nonce is expired");
                }
            } catch (SessionNotFoundException e) {
                throw new UseDpopNonceException(generate(purpose).getNonce(), "DPoP nonce is missing");
            }
        }
    }
}
