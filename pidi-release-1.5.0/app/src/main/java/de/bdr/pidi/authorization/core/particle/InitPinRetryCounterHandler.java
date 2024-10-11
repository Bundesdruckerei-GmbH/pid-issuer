/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProof;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.service.KeyProofService;
import de.bdr.pidi.authorization.core.service.PinProofService;
import de.bdr.pidi.authorization.core.service.PinRetryCounterService;
import de.bdr.pidi.authorization.core.util.PinUtil;
import de.bdr.pidi.base.requests.SeedCredentialRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InitPinRetryCounterHandler implements OidHandler {

    private final PinProofService pinProofService;
    private final KeyProofService keyProofService;
    private final PinRetryCounterService pinRetryCounterService;

    @Override
    public void processSeedCredentialRequest(HttpRequest<SeedCredentialRequest> request, WResponseBuilder response, WSession session) {
        var pinDerivedEphKeyPop = request.getBody().getPinDerivedEphKeyPop();
        var deviceKeyProof = request.getBody().getSingleProof().getSignedJwt();

        var deviceKey = keyProofService.validateJwtProof(session, (JwtProof) request.getBody().getProof(), false);
        var pinDerivedPublicKey = pinProofService.validatePinDerivedEphKeyPop(session, pinDerivedEphKeyPop);

        var dpopJwk = session.getCheckedParameterAsJwk(SessionKey.DPOP_PUBLIC_KEY);

        PinUtil.compareKeys(deviceKey, dpopJwk, "Proof");
        PinUtil.crossCompareKeys(pinDerivedEphKeyPop, deviceKeyProof);

        pinRetryCounterService.initPinRetryCounter(dpopJwk);

        session.putParameter(SessionKey.PIN_DERIVED_PUBLIC_KEY, pinDerivedPublicKey.toJSONString());
    }

}
