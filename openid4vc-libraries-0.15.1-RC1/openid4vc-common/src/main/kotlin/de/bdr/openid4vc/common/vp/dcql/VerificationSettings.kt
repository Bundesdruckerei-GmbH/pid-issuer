/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.dcql

/** Specifies the behaviour of the DCQL query verification check. */
class VerificationSettings(
    /**
     * Defines that additional disclosures that are not required to fulfill the query are allowed.
     * If `false` verification will fail if more disclosures than required are present, otherwise
     * those presentations are considered valid.
     */
    val allowDisclosureOfUnnecessaryClaims: Boolean
)
