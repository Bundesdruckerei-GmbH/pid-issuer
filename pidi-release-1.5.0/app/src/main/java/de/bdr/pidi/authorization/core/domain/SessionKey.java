/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum SessionKey {
    ACCESS_TOKEN("accessToken"),
    ACCESS_TOKEN_EXP_TIME("accessTokenExpirationTime"),
    AUTHORIZATION_CODE("authorizationCode"),
    AUTHORIZATION_CODE_EXP_TIME("authorizationCodeExpirationTime"),
    CLIENT_ID("clientId"),
    CLIENT_INSTANCE_KEY("clientInstanceKey"),
    CODE_CHALLENGE("codeChallenge"),
    CODE_CHALLENGE_METHOD("codeChallengeMethod"),
    C_NONCE("cNonce"),
    C_NONCE_EXP_TIME("cNonceExpirationTime"),
    DEVICE_KEY_PAIR("deviceKeyPair"),
    DPOP_IDENTIFIER("dpopIdentifier"),
    DPOP_NONCE("dpopNonce"),
    DPOP_NONCE_EXP_TIME("dpopNonceExpirationTime"),
    DPOP_PUBLIC_KEY("dpopPublicKey"),
    IDENTIFICATION_DATA("identificationData"),
    IDENTIFICATION_ERROR("identificationError"),
    IDENTIFICATION_RESULT("identificationResult"),
    ISSUER_STATE("issuerState"),
    PID_ISSUER_SESSION_ID("pidIssuerSessionId"),
    PID_ISSUER_SESSION_ID_EXP_TIME("pidIssuerSessionIdExpirationTime"),
    PIN_DERIVED_PUBLIC_KEY("pinDerivedPublicKey"),
    REDIRECT_URI("redirectUri"),
    REFRESH_TOKEN_DIGEST("refreshTokenDigest"),
    REQUEST_URI("requestUri"),
    REQUEST_URI_EXP_TIME("requestUriExpirationTime"),
    SCOPE("scope"),
    STATE("state"),
    VERIFIED_CREDENTIAL_KEY("verifiedCredentialKey"),
    VERIFIED_CREDENTIAL_KEYS("verifiedCredentialKeys");

    @JsonValue
    private final String value;
}
