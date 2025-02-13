/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.credentials

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredential
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredential

/**
 * A general abstraction of a credential. Concrete data elements are to be defined by more specific
 * subtypes.
 *
 * @see JsonCredential
 * @see SdJwtVcCredential
 * @see MsoMdocCredential
 */
interface Credential {
    val format: CredentialFormat
    val issuer: IssuerInfo
    val status: StatusInfo?

    fun withStatus(statusInfo: StatusInfo): Credential

    fun withIssuer(issuer: IssuerInfo): Credential
}
