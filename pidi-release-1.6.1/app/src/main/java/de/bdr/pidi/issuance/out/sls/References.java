/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.issuance.out.sls;

import de.bdr.openid4vc.vci.service.statuslist.StatusReference;

import java.util.List;

public record References(List<StatusReference> references) {
}
