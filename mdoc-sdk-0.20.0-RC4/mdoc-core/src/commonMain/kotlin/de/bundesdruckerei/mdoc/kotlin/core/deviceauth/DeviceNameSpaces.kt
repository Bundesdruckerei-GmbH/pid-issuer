/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.NameSpace
import de.bundesdruckerei.mdoc.kotlin.core.auth.IssuerNameSpaces
import de.bundesdruckerei.mdoc.kotlin.core.auth.KeyAuthorizations
import de.bundesdruckerei.mdoc.kotlin.core.common.CBOR_TAG_BYTE_STRING
import de.bundesdruckerei.mdoc.kotlin.core.common.dataItem
import de.bundesdruckerei.mdoc.kotlin.core.common.log

typealias DeviceNameSpacesBytes = CBORObject

class DeviceNameSpaces(val value: MutableMap<NameSpace, DeviceSignedItems>) {

    private var pristineCBORObject: CBORObject? = null
    fun asCBOR(): CBORObject = pristineCBORObject ?: getEmptyPristineCBORObject()
    private fun getEmptyPristineCBORObject() = CBORObject.NewMap().also {
        value.forEach { (namespace, items) ->
            it.Set(namespace, CBORObject.NewMap().apply {
                items.items.forEach { (name, value) -> Set(name, value) }
            })
        }
    }

    fun asTaggedCBOR(): DeviceNameSpacesBytes =
        CBORObject.FromObjectAndTag(asCBOR().EncodeToBytes(), CBOR_TAG_BYTE_STRING)

    fun asTaggedCBORBytes(): ByteArray = asTaggedCBOR().EncodeToBytes()

    companion object {

        fun fromCBOR(cborObject: CBORObject): DeviceNameSpaces {
            val nameSpaces: MutableMap<NameSpace, DeviceSignedItems> = mutableMapOf()

            for (nameSpace in cborObject.keys) {
                val deviceSignedItemsAsArray = ArrayList<DeviceSignedItem>()
                val cborItems = cborObject[nameSpace]

                for (key in cborItems.keys) {
                    deviceSignedItemsAsArray.add(Pair(key.AsString(), cborItems[key]))
                }

                nameSpaces[nameSpace.AsString()] = DeviceSignedItems(deviceSignedItemsAsArray)
            }

            return DeviceNameSpaces(nameSpaces).also {
                it.pristineCBORObject = cborObject
            }
        }

        fun fromTaggedCBOR(taggedCborObject: CBORObject): DeviceNameSpaces =
            fromCBOR(taggedCborObject.dataItem())

        fun from(
            issuerNameSpaces: IssuerNameSpaces,
            authorizations: KeyAuthorizations?
        ): DeviceNameSpaces {
            val deviceNameSpaces = DeviceNameSpaces(mutableMapOf())

            issuerNameSpaces.forEach { (nameSpace, issuerSignedItems) ->
                val deviceSignedItemsList =
                    issuerSignedItems.map { it.elementIdentifier to it.elementValue }

                deviceNameSpaces.value[nameSpace] =
                    DeviceSignedItems(ArrayList(deviceSignedItemsList))
            }

            val authorizedDeviceNameSpaces = DeviceNameSpaces(mutableMapOf())

            authorizations?.authorizedNameSpaces?.forEach {
                deviceNameSpaces.value.forEach { (nameSpace, items) ->
                    if (it == nameSpace) authorizedDeviceNameSpaces.value[nameSpace] = items
                }
            }

            authorizations?.authorizedDataElements?.forEach { (authNameSpace, authDataElementArray) ->

                if (!authorizedDeviceNameSpaces.value.contains(authNameSpace)) {
                    log.e("Same namespace must not be in authorizedNameSpaces and in authorizedDataElements!")
                }

                deviceNameSpaces.value.forEach { (nameSpace, dataElement) ->

                    val deviceSignedItems = arrayListOf<DeviceSignedItem>()

                    dataElement.items.forEach {
                        if (nameSpace == authNameSpace && authDataElementArray.contains(it.first)) {
                            deviceSignedItems.add(it)
                        }
                    }

                    if (deviceSignedItems.isNotEmpty()) {
                        authorizedDeviceNameSpaces.value[nameSpace] =
                            DeviceSignedItems(deviceSignedItems)
                    }
                }
            }

            return authorizedDeviceNameSpaces
        }
    }
}
