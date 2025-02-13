/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.core.service;

import de.bdr.pidi.base.PidServerException;
import de.bundesdruckerei.mdoc.kotlin.core.SessionTranscript;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import javax.crypto.KeyAgreement;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public class EMacKey {
    private final byte[] key;

    /**
     * @param skS private key of the Signing Party
     * @param pkR public key of the Verifying Party
     */
    public EMacKey(ECPrivateKey skS, ECPublicKey pkR, SessionTranscript sessionTranscript) throws InvalidKeyException {
        var sha265SessionTranscript = performSha256(sessionTranscript);
        var sharedSecret = performDHKA(skS, pkR);
        this.key = performKDF(sharedSecret, sha265SessionTranscript);
    }

    private byte[] performKDF(byte[] sharedSecret, byte[] sha265SessionTranscript) {
        Digest digest = new SHA256Digest();
        HKDFBytesGenerator generator = new HKDFBytesGenerator(digest);
        generator.init(new HKDFParameters(sharedSecret, sha265SessionTranscript, "EMacKey".getBytes(StandardCharsets.UTF_8)));
        var output = new byte[32];
        generator.generateBytes(output, 0, 32);
        return output;
    }

    private byte[] performDHKA(ECPrivateKey skS, ECPublicKey pkR) throws InvalidKeyException {
        try {
            var dhKeyAgreement = KeyAgreement.getInstance("ECDH");
            dhKeyAgreement.init(skS);
            dhKeyAgreement.doPhase(pkR, true);
            return dhKeyAgreement.generateSecret();
        } catch (NoSuchAlgorithmException e) {
            throw new PidServerException("KeyAgreement algorithm invalid", e);
        }
    }

    private byte[] performSha256(SessionTranscript sessionTranscript) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(sessionTranscript.asBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new PidServerException(e.getMessage(), e);
        }
    }

    public byte[] getByte() {
        return key;
    }
}
