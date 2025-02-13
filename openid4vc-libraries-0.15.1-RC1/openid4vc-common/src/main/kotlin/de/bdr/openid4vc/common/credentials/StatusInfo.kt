/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.credentials

interface StatusInfo

data class StatusListStatusInfo(val uri: String, val index: Int, val status: Byte?) : StatusInfo
