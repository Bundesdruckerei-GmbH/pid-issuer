/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.COSEKey
import de.bundesdruckerei.mdoc.kotlin.core.any
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.int

typealias KeyInfo = MutableMap<int, any>

data class DeviceKeyInfo(
    val deviceKey: COSEKey,
    val keyAuthorizations: KeyAuthorizations? = null,
    val keyInfo: KeyInfo? = null
) : ICBORable {

    override fun asCBOR(): CBORObject {
        return CBORObject.NewMap().apply {
            Set("deviceKey", deviceKey.AsCBOR())
            keyAuthorizations?.let { Set("keyAuthorizations", it.asCBOR()) }
            keyInfo
                ?.filter { entry -> entry.key < 0 }
                ?.let { Set("keyInfo", it) }
        }
    }

    companion object {

        fun fromCBOR(cborObject: CBORObject): DeviceKeyInfo {
            val deviceKey = COSEKey(cborObject["deviceKey"])
            // deviceKey = CoseKey cborObject deviceKey

            var keyAuthorizations: KeyAuthorizations? = null
            cborObject["keyAuthorizations"]?.let {
                keyAuthorizations =
                    KeyAuthorizations.fromCBOR(it)
            }

            val keyInfo: KeyInfo? = cborObject["keyInfo"]?.run {
                mutableMapOf<int, any>().also {
                    entries
                        .filter { entry -> entry.key.AsInt32Value() < 0 }
                        .forEach { (key, value) ->
                            it[key.AsInt32Value()] = value
                        }
                }
            }

            return DeviceKeyInfo(deviceKey, keyAuthorizations, keyInfo)
        }
    }
}
