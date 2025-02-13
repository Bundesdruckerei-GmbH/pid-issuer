/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.vci.service.credential.response.encryption

import de.bdr.openid4vc.common.vci.CredentialEncryption

interface CredentialResponseEncryptionService {

    fun encrypt(data: String, credentialEncryption: CredentialEncryption): String

    val algValuesSupported: List<String>

    val encValuesSupported: List<String>

    val required: Boolean
}
