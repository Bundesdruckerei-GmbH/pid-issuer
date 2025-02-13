/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.util;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.SignedJWT;
import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.util.Objects;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PinUtil {
    private static final String INVALID_MSG = "%s invalid";
    public static final String JWT_DEVICE_CLAIM = "device_key";
    public static final String JWT_PIN_CLAIM = "pin_derived_eph_pub";
    private static final String JWT_JWK_CLAIM = "jwk";

    public static void crossCompareKeys(SignedJWT pinDerivedEphKeyPop, SignedJWT deviceKeyPop) {
        var pinKey = pinDerivedEphKeyPop.getHeader().getJWK();
        var deviceKey = deviceKeyPop.getHeader().getJWK();
        JWK pinPopDeviceKey;
        try {
            pinPopDeviceKey = JWK.parse(JSONObjectUtils.getJSONObject(pinDerivedEphKeyPop.getJWTClaimsSet().getJSONObjectClaim(JWT_DEVICE_CLAIM), JWT_JWK_CLAIM));
        } catch (ParseException e) {
            log.info("Pin JWT claim {} could not be parsed", JWT_DEVICE_CLAIM);
            throw new InvalidRequestException(INVALID_MSG.formatted(JWT_PIN_CLAIM));
        }
        JWK devicePopPinKey;
        try {
            devicePopPinKey = JWK.parse(JSONObjectUtils.getJSONObject(deviceKeyPop.getJWTClaimsSet().getJSONObjectClaim(JWT_PIN_CLAIM), JWT_JWK_CLAIM));
        } catch (ParseException e) {
            log.info("Device JWT claim {} could not be parsed", JWT_PIN_CLAIM);
            throw new InvalidRequestException(INVALID_MSG.formatted(JWT_DEVICE_CLAIM));
        }
        if (!Objects.equals(pinKey, devicePopPinKey)) {
            log.info("JWT pin keys do not match");
            throw new InvalidRequestException(INVALID_MSG.formatted(JWT_PIN_CLAIM));
        }
        if (!Objects.equals(deviceKey, pinPopDeviceKey)) {
            log.info("JWT device keys do not match");
            throw new InvalidRequestException(INVALID_MSG.formatted(JWT_DEVICE_CLAIM));
        }
    }

    public static void compareKeys(JWK key1, JWK key2, String propertyName) {
        if (!Objects.equals(key1, key2)) {
            throw new InvalidRequestException(INVALID_MSG.formatted(propertyName));
        }
    }

    public static SignedJWT parseBody(String jwt, String propertyName) {
        try {
            return SignedJWT.parse(jwt);
        } catch (ParseException e) {
            throw new InvalidRequestException(propertyName + " not a valid JWT");
        }
    }

    public static SignedJWT parseBody(HttpRequest<?> request, String propertyName) {
        return parseBody(RequestUtil.getParam(request, propertyName), propertyName);
    }

    public static String computeRetryCounterId(JWK clientInstanceKey) {
        return DigestUtil.computeDigest(clientInstanceKey.toJSONString());
    }
}
