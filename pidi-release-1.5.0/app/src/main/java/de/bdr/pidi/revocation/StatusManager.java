/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.revocation;

import java.math.BigDecimal;

public interface StatusManager {

    BigDecimal acquireFreeIndex();

}
