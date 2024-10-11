/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.service;

import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.base.PidServerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PidSerializerTest {

    private final PidCredentialData pid = ServiceTestData.createPid();
    private final PidSerializer out = new PidSerializer();

    @DisplayName("encode a PID, OK")
    @Test
    void test001() {
        var result = out.toString(pid);
        assertNotEquals(0, result.indexOf("Zwergen"));
    }

    @DisplayName("decode a PID, OK")
    @Test
    void test002() {
        // WIP
        var pidString = ServiceTestData.createPidString();

        var result = out.fromString(pidString);
        assertEquals("Rhein", result.getBirthFamilyName());
        assertEquals("DE", result.getNationality());
    }

    @DisplayName("decode a PID, fail")
    @Test
    void test003() {
        // WIP
        var pidString = ServiceTestData.createPidString_withBrokenCountryCode();
        assertThrows(PidServerException.class, () -> out.fromString(pidString));
    }
}
