/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.revocation.core;

import de.bdr.pidi.revocation.StatusManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StatusManagerImpl
        implements StatusManager {

    @Override
    public BigDecimal acquireFreeIndex() {
        return BigDecimal.valueOf(1);
    }
}
