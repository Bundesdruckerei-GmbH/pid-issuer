/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.credentials

import de.bdr.openid4vc.common.vci.InputMode
import de.bdr.openid4vc.common.vci.TxCodeMetadata
import de.bdr.openid4vc.vci.utils.randomString
import kotlinx.serialization.Serializable

@Serializable
class TxCode(val code: String, val metadata: TxCodeMetadata) {

    companion object {
        private val VALID_NUMERIC_TX_CODE = Regex("[0-9]{4,8}")
        private val digits = "0123456789".toCharArray()

        fun randomNumeric(length: Int = 8) =
            TxCode(
                randomString(length, digits),
                TxCodeMetadata(inputMode = InputMode.NUMERIC, length = length)
            )
    }

    init {
        check(metadata.inputMode == InputMode.TEXT || code.matches(VALID_NUMERIC_TX_CODE)) {
            "Invalid transaction code $code"
        }
    }
}
