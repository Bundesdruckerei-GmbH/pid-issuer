/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable

class DeviceAuth(val devAuth: DevAuthBase<*, *>) : ICBORable {
    override fun asCBOR(): CBORObject = CBORObject.NewMap()
        .Set(devAuth.contextTag.tag, devAuth.asCBOR())

    companion object {
        fun fromCBOR(cborObject: CBORObject): DeviceAuth {
            val contextTag = cborObject.keys?.firstOrNull()?.AsString()
                ?: throw IllegalArgumentException("ContextTag should not be null.")
            val devAuthObject = cborObject[contextTag]

            return DeviceAuth(
                DevAuthBase.fromCBOR(
                    devAuthObject,
                    ContextTag.fromString(contextTag)
                )
            )
        }
    }
}
