/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.pex

import de.bdr.openid4vc.common.CredentialFormat
import kotlinx.serialization.Serializable

@Serializable(with = FormatDescriptionSerializer::class)
interface FormatDescription {
    val type: CredentialFormat
}
