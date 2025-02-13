/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import de.bundesdruckerei.mdoc.kotlin.core.DataElementIdentifier
import de.bundesdruckerei.mdoc.kotlin.core.DataElementValue

typealias DeviceSignedItem = Pair<DataElementIdentifier, DataElementValue>

data class DeviceSignedItems(val items: ArrayList<DeviceSignedItem>)
