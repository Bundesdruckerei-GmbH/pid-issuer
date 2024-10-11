/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import de.bundesdruckerei.mdoc.kotlin.core.DataElementIdentifier
import de.bundesdruckerei.mdoc.kotlin.core.DataElementValue

typealias DeviceSignedItem = Pair<DataElementIdentifier, DataElementValue>

data class DeviceSignedItems(val items: ArrayList<DeviceSignedItem>)
