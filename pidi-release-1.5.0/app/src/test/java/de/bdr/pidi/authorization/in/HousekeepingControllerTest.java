/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.in;

import de.bdr.pidi.authorization.core.AuthorizationHousekeeping;
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
    AuthorizationHousekeeping authorizationHousekeeping;
    @InjectMocks
    HousekeepingController housekeepingController;

    @DisplayName("Verify session cleanup is called")
    @Test
    void test001() {
        housekeepingController.housekeeping();
        verify(authorizationHousekeeping).cleanupExpiredSessions();
    }
}