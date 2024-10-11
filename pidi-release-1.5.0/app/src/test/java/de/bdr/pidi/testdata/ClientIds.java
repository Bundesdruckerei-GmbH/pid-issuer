/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.testdata;

import java.util.List;
import java.util.UUID;

public class ClientIds {
    public static final List<UUID> validClientIds = List.of(
            UUID.fromString("fed79862-af36-4fee-8e64-89e3c91091ed"));

    public static UUID validClientId() {
        return validClientIds.getFirst();
    }
}
