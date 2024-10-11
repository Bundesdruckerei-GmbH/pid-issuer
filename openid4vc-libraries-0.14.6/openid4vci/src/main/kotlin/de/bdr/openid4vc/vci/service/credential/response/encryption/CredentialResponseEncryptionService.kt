/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.service.credential.response.encryption

import de.bdr.openid4vc.common.vci.CredentialEncryption

interface CredentialResponseEncryptionService {

    fun encrypt(data: String, credentialEncryption: CredentialEncryption): String

    val algValuesSupported: List<String>

    val encValuesSupported: List<String>

    val required: Boolean
}
