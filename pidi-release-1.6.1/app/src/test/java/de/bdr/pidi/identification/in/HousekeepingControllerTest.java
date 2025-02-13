/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.in;

import de.bdr.pidi.identification.core.IdentificationHousekeeping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HousekeepingControllerTest {

    @Mock
    IdentificationHousekeeping identificationHousekeeping;
    @InjectMocks
    HousekeepingController housekeepingController;

    @DisplayName("Verify authentication cleanup is called")
    @Test
    void test001() {
        housekeepingController.housekeeping();
        verify(identificationHousekeeping).cleanupExpiredAuthentications();
    }
}