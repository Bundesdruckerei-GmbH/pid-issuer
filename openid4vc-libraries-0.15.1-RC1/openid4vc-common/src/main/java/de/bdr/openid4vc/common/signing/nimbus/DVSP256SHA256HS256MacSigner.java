/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.signing.nimbus;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.impl.AlgorithmSupportMessage;
import com.nimbusds.jose.crypto.impl.HMAC;
import com.nimbusds.jose.crypto.impl.MACProvider;
import com.nimbusds.jose.util.Base64URL;
import de.bdr.openid4vc.common.JWSAlgorithms;

import java.util.Set;

/**
 * Intended to be moved upstream to nimbus. Should be integrated in {@link com.nimbusds.jose.crypto.MACSigner}
 */
public class DVSP256SHA256HS256MacSigner extends MACProvider implements JWSSigner {

    public static final Set<JWSAlgorithm> SUPPORTED_ALGORITHMS = Set.of(JWSAlgorithms.getDVS_P256_SHA256_HS256());
    private final DVSP256SHA256Key dvsp256SHA256Key;

    public DVSP256SHA256HS256MacSigner(DVSP256SHA256Key dvsp256SHA256Key) throws KeyLengthException {
        super(dvsp256SHA256Key.getByte());

        this.dvsp256SHA256Key = dvsp256SHA256Key;
    }

    /**
     * Gets the matching Java Cryptography Architecture (JCA) algorithm
     * name for the specified HMAC-based JSON Web Algorithm (JWA).
     *
     * @param alg The JSON Web Algorithm (JWA). Must be supported and not
     *            {@code null}.
     * @return The matching JCA algorithm name.
     * @throws JOSEException If the algorithm is not supported.
     */
    protected static String getJCAAlgorithmName(final JWSAlgorithm alg)
            throws JOSEException {

        if (alg.equals(JWSAlgorithms.getDVS_P256_SHA256_HS256())) {
            return "HMACSHA256";
        } else {
            throw new JOSEException(AlgorithmSupportMessage.unsupportedJWSAlgorithm(
                    alg,
                    SUPPORTED_ALGORITHMS));
        }
    }

    @Override
    public Base64URL sign(JWSHeader jwsHeader, final byte[] signingInput) throws JOSEException {
        byte[] hmac = HMAC.compute(getJCAAlgorithmName(jwsHeader.getAlgorithm()), getSecretKey(), signingInput, getJCAContext().getProvider());
        return Base64URL.encode(hmac);
    }

    public DVSP256SHA256Key getDvsp256SHA256Key() {
        return dvsp256SHA256Key;
    }
}
