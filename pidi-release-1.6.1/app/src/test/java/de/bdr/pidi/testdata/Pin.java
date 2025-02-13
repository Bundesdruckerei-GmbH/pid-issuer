/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.testdata;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import de.bdr.pidi.authorization.FlowVariant;

public final class Pin {
    private final JWK clientInstanceKey;
    private final ECKey pinDerivedKeyPair;
    private SignedJWT deviceKeyProof;
    private SignedJWT pinDerivedEphKeyPop;
    private SignedJWT deviceKeyPop;
    private final ECKey deviceKeyPair;

    private final FlowVariant flowVariant = FlowVariant.B1;


    private Pin(String pinNonce, ECKey deviceKeyPair, ECKey pinDerivedKeyPair) {
        this.deviceKeyPair = deviceKeyPair;
        this.clientInstanceKey = deviceKeyPair.toPublicJWK();
        this.pinDerivedKeyPair = pinDerivedKeyPair;
        updateNonce(pinNonce);
    }

    /**
     * generates a pinDerivedKeyPair which provides the pinDerivedPublicKey in the deviceKeySignedNonce
     */
    public static Pin createPin(String pinNonce, ECKey deviceKeyPair) {
        return new Pin(pinNonce, deviceKeyPair, TestUtils.generateEcKey());
    }

    /**
     * defaults the deviceKeyPair to {@link TestUtils}.DEVICE_KEY_PAIR,
     * therefore it is required that wallet attestation uses its public key as CLIENT_INSTANCE_KEY
     *
     * @see Pin#createPin(String, ECKey)
     */
    public static Pin createPin(String pinNonce) {
        return Pin.createPin(pinNonce, TestUtils.DEVICE_KEY_PAIR);
    }

    public void updateNonce(String pinNonce) {
        var pinDerivedPublicKey = pinDerivedKeyPair.toPublicJWK();
        this.deviceKeyProof = TestUtils.buildSeedCredentialProof(pinNonce, flowVariant, pinDerivedPublicKey, deviceKeyPair);
        this.pinDerivedEphKeyPop = TestUtils.buildPinDerivedEphKeyPop(pinNonce, flowVariant, clientInstanceKey, pinDerivedKeyPair);
        this.deviceKeyPop = TestUtils.buildDeviceKeyPop(pinNonce, flowVariant, pinDerivedPublicKey, deviceKeyPair);
    }

    /**
     * deviceKeyProof.header.alg = "ES256"
     * deviceKeyProof.header.typ = "openid4vci-proof+jwt"
     * deviceKeyProof.header.jwk = Jwk(device_pub)
     * deviceKeyProof.claims.nonce = c_nonce
     * deviceKeyProof.claims.aud = credential_issuer_identifier
     * deviceKeyProof.claims.iat = issued_at
     * deviceKeyProof.claims.pin_derived_eph_pub = Jwk(pin_derived_eph_pub)
     * sign (deviceKeyProof, device_priv)
     */
    public SignedJWT deviceKeyProof() {
        return deviceKeyProof;
    }

    /**
     * pinDerivedEphKeyPop.header.alg = "ES256"
     * pinDerivedEphKeyPop.header.typ = "pin_derived_eph_key_pop"
     * pinDerivedEphKeyPop.header.jwk = Jwk(pin_derived_eph_pub)
     * pinDerivedEphKeyPop.claims.nonce = c_nonce
     * pinDerivedEphKeyPop.claims.pidi_session_id = pidi_session_id
     * pinDerivedEphKeyPop.claims.aud = credential_issuer_identifier
     * pinDerivedEphKeyPop.claims.device_key = Jwk(device_pub)
     * sign (pinDerivedEphKeyPop, pin_derived_eph_pri)
     */
    public SignedJWT pinDerivedEphKeyPop() {
        return pinDerivedEphKeyPop;
    }

    /**
     * deviceKeyProof.header.alg = "ES256"
     * deviceKeyProof.header.typ = "device_key_pop"
     * deviceKeyProof.header.jwk = Jwk(device_pub)
     * pinDerivedEphKeyPop.claims.pidi_session_id = pidi_session_id
     * deviceKeyProof.claims.aud = credential_issuer_identifier
     * deviceKeyProof.claims.pin_derived_eph_pub = Jwk(pin_derived_eph_pub)
     * sign (deviceKeyProof, device_priv)
     */
    public SignedJWT deviceKeyPop() {
        return deviceKeyPop;
    }
}
