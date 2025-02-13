/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.core;

import com.nimbusds.jose.CriticalHeaderParamsAware;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.impl.AlgorithmSupportMessage;
import com.nimbusds.jose.crypto.impl.CriticalHeaderParamsDeferral;
import com.nimbusds.jose.crypto.impl.HMAC;
import com.nimbusds.jose.crypto.impl.MACProvider;
import com.nimbusds.jose.crypto.utils.ConstantTimeUtils;
import com.nimbusds.jose.util.Base64URL;
import de.bdr.openid4vc.common.JWSAlgorithms;
import de.bdr.openid4vc.common.signing.nimbus.DVSP256SHA256Key;
import java.util.Set;

public class DvsVerifier extends MACProvider implements JWSVerifier, CriticalHeaderParamsAware {
    public static final Set<JWSAlgorithm> SUPPORTED_ALGORITHMS = Set.of(JWSAlgorithms.getDVS_P256_SHA256_HS256());

    private final CriticalHeaderParamsDeferral critPolicy = new CriticalHeaderParamsDeferral();

    public DvsVerifier(final DVSP256SHA256Key dvsp256SHA256Key, final Set<String> defCritHeaders) throws KeyLengthException {
        super(dvsp256SHA256Key.getByte());
        this.critPolicy.setDeferredCriticalHeaderParams(defCritHeaders);
    }

    public DvsVerifier(final DVSP256SHA256Key dvsp256SHA256Key) throws KeyLengthException {
        this(dvsp256SHA256Key, null);
    }

    @Override
    public Set<String> getProcessedCriticalHeaderParams() {
        return critPolicy.getProcessedCriticalHeaderParams();
    }

    @Override
    public Set<String> getDeferredCriticalHeaderParams() {
        return critPolicy.getProcessedCriticalHeaderParams();
    }

    protected static String getJCAAlgorithmName(final JWSAlgorithm alg) throws JOSEException {
        if (alg.equals(JWSAlgorithms.getDVS_P256_SHA256_HS256())) {
            return "HMACSHA256";
        } else {
            throw new JOSEException(AlgorithmSupportMessage.unsupportedJWSAlgorithm(
                    alg,
                    SUPPORTED_ALGORITHMS));
        }
    }

    @Override
    public boolean verify(final JWSHeader header,
                          final byte[] signedContent,
                          final Base64URL signature)
            throws JOSEException {
        if (!critPolicy.headerPasses(header)) {
            return false;
        }
        String jcaAlg = getJCAAlgorithmName(header.getAlgorithm());
        byte[] expectedHMAC = HMAC.compute(jcaAlg, getSecretKey(), signedContent, getJCAContext().getProvider());
        return ConstantTimeUtils.areEqual(expectedHMAC, signature.decode());
    }
}
