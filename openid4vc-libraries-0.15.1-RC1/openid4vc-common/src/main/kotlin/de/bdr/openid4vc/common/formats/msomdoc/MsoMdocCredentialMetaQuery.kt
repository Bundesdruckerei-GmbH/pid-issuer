/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.msomdoc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MsoMdocCredentialMetaQuery(@SerialName("doctype_values") val doctypeValues: Set<String>)
