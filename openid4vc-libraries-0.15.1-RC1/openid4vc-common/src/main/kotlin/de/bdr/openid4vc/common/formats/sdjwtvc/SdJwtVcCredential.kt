/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.sdjwtvc

import de.bdr.openid4vc.common.credentials.IssuerInfo
import de.bdr.openid4vc.common.credentials.JsonCredential
import de.bdr.openid4vc.common.credentials.StatusInfo
import java.time.Instant

interface SdJwtVcCredential : JsonCredential {
    val vct: String
    val issuedAt: Instant?
    val validFrom: Instant?
    val expiresAt: Instant?

    override fun withStatus(status: StatusInfo): SdJwtVcCredential

    override fun withIssuer(issuer: IssuerInfo): SdJwtVcCredential
}
