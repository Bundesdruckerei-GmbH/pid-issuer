/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.exception.InvalidGrantException;
import de.bdr.pidi.authorization.core.exception.OIDException;
import de.bdr.pidi.authorization.core.service.PidSerializer;
import de.bdr.pidi.authorization.core.service.PinProofService;
import de.bdr.pidi.authorization.core.service.PinRetryCounterService;
import de.bdr.pidi.authorization.core.util.PinUtil;
import de.bdr.pidi.authorization.core.util.RequestUtil;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.SeedException;
import de.bdr.pidi.authorization.out.issuance.SeedPidBuilder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SeedCredentialValidationHandler implements OidHandler {
    // Request keys
    public static final String SEED_CREDENTIAL = "seed_credential";
    public static final String PIN_DERIVED_EPH_KEY_POP = "pin_derived_eph_key_pop";
    public static final String DEVICE_KEY_POP = "device_key_pop";

    static final String RESULT_OK = "Success";
    private static final String PIN_PROP = "PIN";

    private final SeedPidBuilder seedPidBuilder;
    private final PidSerializer pidSerializer;
    private final String credentialIssuerIdentifier;
    private final PinRetryCounterService pinRetryCounterService;
    private final PinProofService pinProofService;

    @Override
    public void processSeedCredentialTokenRequest(HttpRequest<?> request, WResponseBuilder response, WSession session) {
        String seedCredential = RequestUtil.getParam(request, SEED_CREDENTIAL);
        SignedJWT pinDerivedEphKeyPop = PinUtil.parseBody(request, PIN_DERIVED_EPH_KEY_POP);
        SignedJWT deviceKeyPop = PinUtil.parseBody(request, DEVICE_KEY_POP);

        final SeedPidBuilder.PinSeedData seedData;
        try {
            seedData = seedPidBuilder.extractVerifiedPinSeed(seedCredential, credentialIssuerIdentifier);
        } catch (SeedException e) {
            throw new InvalidGrantException("Seed credential invalid", e);
        }
        JWK pinDerivedPublicKey = seedData.pinDerivedKey();
        JWK dpopJwk = session.getCheckedParameterAsJwk(SessionKey.DPOP_PUBLIC_KEY);
        if (!seedData.clientInstanceKey().equals(dpopJwk)) {
            throw new InvalidGrantException("Seed credential invalid");
        }
        var pinRetryCounterId = pinRetryCounterService.loadPinCounter(dpopJwk);

        try {
            JWK popPinDerivedEphKey = pinProofService.validatePinDerivedEphKeyPopTokenRequest(session, pinDerivedEphKeyPop);
            JWK popDeviceKey = pinProofService.validateDeviceKeyPopTokenRequest(session, deviceKeyPop);
            PinUtil.crossCompareKeys(pinDerivedEphKeyPop, deviceKeyPop);
            PinUtil.compareKeys(popPinDerivedEphKey, pinDerivedPublicKey, PIN_PROP);
            PinUtil.compareKeys(popDeviceKey, dpopJwk, PIN_PROP);
        } catch (OIDException e) {
            pinRetryCounterService.increment(pinRetryCounterId, e);
            throw new InvalidGrantException(PIN_PROP + " invalid", e);
        }

        storePidData(session, seedData.pidCredentialData());
    }

    private void storePidData(WSession session, PidCredentialData pidData) {
        var serialized = pidSerializer.toString(pidData);
        session.putParameter(SessionKey.IDENTIFICATION_RESULT, RESULT_OK);
        session.putParameter(SessionKey.IDENTIFICATION_DATA, serialized);
    }
}
