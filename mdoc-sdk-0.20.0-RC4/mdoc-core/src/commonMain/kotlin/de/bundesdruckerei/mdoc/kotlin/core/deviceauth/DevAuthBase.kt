/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import COSE.Message
import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.bstr
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable

typealias EMacKey = bstr

sealed class DevAuthBase<T : Message, K>(protected val message: T) : ICBORable {
    internal abstract val contextTag: ContextTag

    protected abstract fun validate(key: K): Boolean

    override fun asCBOR(): CBORObject = message.EncodeToCBORObject()

    fun isValid(deviceAuthentication: DeviceAuthentication, key: K): Boolean {
        message.SetContent(deviceAuthentication.asBytes())
        return validate(key)
    }

    companion object {
        fun fromCBOR(obj: CBORObject, tag: ContextTag): DevAuthBase<*, *> {
            return when (tag) {
                ContextTag.Mac -> DeviceMac(obj)
                ContextTag.Sig -> DeviceSignature(obj)
            }
        }
    }
}
