/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth


import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class DeviceSignedItemsTest {

    private val deviceSignedItem = ArrayList<DeviceSignedItem>()
    private lateinit var deviceSignedItems: DeviceSignedItems

    @Rule
    @JvmField
    var thrown: ExpectedException = ExpectedException.none()

    @Before
    fun setUp() {
        deviceSignedItem.add(Pair("Name", "Value"))
        deviceSignedItem.add(Pair("Name2", 123456))
        deviceSignedItems = DeviceSignedItems(deviceSignedItem)
    }

    @Test
    fun getItems() {
        assertEquals(Pair("Name", "Value"), deviceSignedItems.items[0])
        assertEquals(Pair("Name2", 123456), deviceSignedItems.items[1])
    }

    @Test
    fun emptyDeviceSignedItemsShouldReturnEmptyArray() {
        val deviceSignedItemEmpty = ArrayList<DeviceSignedItem>()

        assertEquals(arrayListOf<DeviceSignedItem>(), DeviceSignedItems(deviceSignedItemEmpty).items)
    }

    @Test
    fun copy() {
        assertEquals(deviceSignedItems, deviceSignedItems.copy())
    }
}
