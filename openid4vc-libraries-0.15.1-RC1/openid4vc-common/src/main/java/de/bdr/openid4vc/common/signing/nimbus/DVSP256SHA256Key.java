/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.signing.nimbus;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import javax.crypto.KeyAgreement;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public class DVSP256SHA256Key {
    private final byte[] key;
    private final ECPublicKey pkR;
    /**
     * @param skS private key of the Signing Party
     * @param pkR public key of the Verifying Party
     */
    public DVSP256SHA256Key(ECPrivateKey skS, ECPublicKey pkR) throws NoSuchAlgorithmException, InvalidKeyException {
        var sharedSecret = performDHKA(skS, pkR);
        this.key = performKDF(sharedSecret);
        this.pkR = pkR;
    }

    private byte[] performKDF(byte[] sharedSecret) {
        Digest digest = new SHA256Digest();
        HKDFBytesGenerator generator = new HKDFBytesGenerator(digest);
        generator.init(new HKDFParameters(sharedSecret, null, "DVS-1".getBytes(StandardCharsets.UTF_8)));
        var output = new byte[32];
        generator.generateBytes(output, 0, 32);
        return output;
    }

    private byte[] performDHKA(ECPrivateKey skS, ECPublicKey pkR) throws NoSuchAlgorithmException, InvalidKeyException {
        var dhKeyAgreement = KeyAgreement.getInstance("ECDH");
        dhKeyAgreement.init(skS);
        dhKeyAgreement.doPhase(pkR, true);
        return dhKeyAgreement.generateSecret();
    }

    public ECPublicKey getPkR() {
        return pkR;
    }

    public byte[] getByte() {
        return key;
    }
}