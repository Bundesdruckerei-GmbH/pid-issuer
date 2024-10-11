/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.in;

import de.bdr.pidi.identification.core.IdentificationHousekeeping;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.stereotype.Component;

@Component("identificationHousekeepingController")
@WebEndpoint(id = "identHousekeeping")
public class HousekeepingController {

    private final IdentificationHousekeeping identificationHousekeeping;

    public HousekeepingController(IdentificationHousekeeping identificationHousekeeping) {
        this.identificationHousekeeping = identificationHousekeeping;
    }

    @WriteOperation
    public void housekeeping() {
        identificationHousekeeping.cleanupExpiredAuthentications();
    }
}
