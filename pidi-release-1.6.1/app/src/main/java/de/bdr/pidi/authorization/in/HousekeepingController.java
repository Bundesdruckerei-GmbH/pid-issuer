/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.in;

import de.bdr.pidi.authorization.core.AuthorizationHousekeeping;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.stereotype.Component;

@Component("authorizationHousekeepingController")
@WebEndpoint(id = "authHousekeeping")
public class HousekeepingController {

    private final AuthorizationHousekeeping authorizationHousekeeping;

    public HousekeepingController(AuthorizationHousekeeping authorizationHousekeeping) {
        this.authorizationHousekeeping = authorizationHousekeeping;
    }

    @WriteOperation
    public void housekeeping() {
        authorizationHousekeeping.cleanupExpiredSessions();
    }
}
