/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.in;

import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Base64;

import static de.bdr.pidi.base.FileResourceHelper.getFileInputStream;
import static de.bdr.pidi.issuance.ConfigTestData.ISSUANCE_CONFIG;

class MdocBuilderBase extends PidCredentialDataBase {
    protected static final CBORMapper CBOR_MAPPER = new CBORMapper();
    protected static final Duration LIFETIME = Duration.ofDays(14L);
    protected static final String DOC_TYPE = "eu.europa.ec.eudi.pid.1";
    private static final String ISSUANCE_TEST_KEYSTORE_RESOURCES_PATH = "/keystore/issuance-test-keystore.p12";

    protected static byte[] getDecodedString(String encoded) {
        return Base64.getUrlDecoder().decode(encoded);
    }

    /**
     * CBOR Object Signing and Encryption header parameters according to
     * <a href="https://www.iana.org/assignments/cose/cose.xhtml">CBOR Object Signing and Encryption (COSE)</a>
     */
    protected enum CoseHeaderParameters {
        ALG(1), X5CHAIN(33);

        final int label;

        CoseHeaderParameters(int label) {
            this.label = label;
        }
    }

    /**
     * CBOR Object Signing and Encryption algorithms according to
     * <a href="https://www.iana.org/assignments/cose/cose.xhtml">CBOR Object Signing and Encryption (COSE)</a>
     */
    protected enum CoseAlgorithms {
        ECDSA(-7);

        final int value;

        CoseAlgorithms(int value) {
            this.value = value;
        }
    }

    protected enum CoseKeyTypes {
        EC2(2);

        final int value;

        CoseKeyTypes(int value) {
            this.value = value;
        }
    }

    protected enum CoseKeyCommonParameters {
        KTY(1), X(-2), Y(-3);

        final int label;

        CoseKeyCommonParameters(int label) {
            this.label = label;
        }

        String labelStr() {
            return String.valueOf(label);
        }
    }

    protected Certificate[] givenDeviceBindingKeyCertificateChain() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        var userBindingKeystore = KeyStore.getInstance("PKCS12");
        userBindingKeystore.load(this.getClass().getResourceAsStream(ISSUANCE_TEST_KEYSTORE_RESOURCES_PATH), ISSUANCE_CONFIG.getSignerPassword().toCharArray());

        return userBindingKeystore.getCertificateChain(userBindingKeystore.aliases().asIterator().next());
    }

    protected static PublicKey getSignerPub() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        KeyStore ks = KeyStore.getInstance("pkcs12");
        ks.load(getFileInputStream(ISSUANCE_CONFIG.getSignerPath()), ISSUANCE_CONFIG.getSignerPassword().toCharArray());
        return ks.getCertificate(ISSUANCE_CONFIG.getSignerAlias()).getPublicKey();
    }
}
