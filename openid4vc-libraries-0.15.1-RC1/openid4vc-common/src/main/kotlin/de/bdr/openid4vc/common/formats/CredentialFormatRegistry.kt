/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialFormat
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialFormat

internal object CredentialFormatRegistry {

    val registry = mutableMapOf<String, CredentialFormat>()

    init {
        SdJwtVcCredentialFormat.register()
        MsoMdocCredentialFormat.register()
    }
}
