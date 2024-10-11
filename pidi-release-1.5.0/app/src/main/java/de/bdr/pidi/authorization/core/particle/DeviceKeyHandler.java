/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import de.bdr.openid4vc.common.vci.CredentialRequest;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.WResponseBuilder;
import de.bdr.pidi.authorization.core.WSession;
import de.bdr.pidi.authorization.core.domain.SessionKey;
import de.bdr.pidi.authorization.core.util.RandomUtil;
import de.bdr.pidi.base.PidServerException;

public class DeviceKeyHandler implements OidHandler {
    private final JWKGenerator<ECKey> keyGenerator = new ECKeyGenerator(Curve.P_256).keyUse(KeyUse.SIGNATURE);

    @Override
    public void processCredentialRequest(HttpRequest<CredentialRequest> request, WResponseBuilder response, WSession session) {
        try {
            ECKey keyPair = keyGenerator.keyID(RandomUtil.randomString()).generate();
            session.putParameter(SessionKey.DEVICE_KEY_PAIR, keyPair.toJSONString());
        } catch (JOSEException e) {
            throw new PidServerException("Could not create ephemeral device keypair", e);
        }
    }
}
