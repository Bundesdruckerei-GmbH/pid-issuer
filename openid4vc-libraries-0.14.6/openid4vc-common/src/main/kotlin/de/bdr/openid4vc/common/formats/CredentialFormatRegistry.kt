/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
