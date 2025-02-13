/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.end2end.requests;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.dpop.DPoPProofFactory;
import com.nimbusds.oauth2.sdk.dpop.DPoPUtils;
import com.nimbusds.oauth2.sdk.dpop.DefaultDPoPProofFactory;
import com.nimbusds.oauth2.sdk.id.JWTID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.Nonce;

import java.net.URI;
import java.util.Date;

public class TestDPopProofFactory extends DefaultDPoPProofFactory {
    private final JOSEObjectType type;

    public TestDPopProofFactory(JWK jwk, JWSAlgorithm jwsAlg) throws JOSEException {
        super(jwk, jwsAlg);
        this.type = DPoPProofFactory.TYPE;
    }

    public TestDPopProofFactory(JWK jwk, JWSAlgorithm jwsAlg, JOSEObjectType type) throws JOSEException {
        super(jwk, jwsAlg);
        this.type = type;
    }

    @Override
    public SignedJWT createDPoPJWT(final JWTID jti,
                                   final String htm,
                                   final URI htu,
                                   final Date iat,
                                   final AccessToken accessToken,
                                   final Nonce nonce)
            throws JOSEException {

        JWSHeader jwsHeader = new JWSHeader.Builder(getJWSAlgorithm())
                .type(type)
                .jwk(getPublicJWK())
                .build();

        JWTClaimsSet jwtClaimsSet = DPoPUtils.createJWTClaimsSet(jti, htm, htu, iat, accessToken, nonce);
        SignedJWT signedJWT = new SignedJWT(jwsHeader, jwtClaimsSet);
        signedJWT.sign(getJWSSigner());
        return signedJWT;
    }
}
