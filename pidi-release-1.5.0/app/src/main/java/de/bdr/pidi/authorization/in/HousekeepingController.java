/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
