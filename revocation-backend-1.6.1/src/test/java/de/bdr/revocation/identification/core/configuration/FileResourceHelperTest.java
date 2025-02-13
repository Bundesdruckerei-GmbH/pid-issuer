/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileResourceHelperTest {

    private FileResourceHelper out;

    @BeforeEach
    void setup() {
        out = new FileResourceHelper();
    }

    @Test
    void checkMissingCertificate() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> out.readCertificate("nothing"));
    }

    @Test
    void checkMissingKeyStore() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> out.readKeyStore("nothing", "wrong"));
    }

    @Test
    void checkMalformedCertificate() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> out.readCertificate("./keys/test/sign.localhost_2382.p12"));
    }

    @Test
    void checkMalformedKeyStore() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> out.readKeyStore("./keys/test/demo.governikus-eid.encr.cer", "bad"));
    }

    @Test
    void checkBadPasswordKeyStore() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> out.readKeyStore("./keys/test/sign.localhost_2382.p12", "bad"));
    }

}