/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.DataElementIdentifier
import de.bundesdruckerei.mdoc.kotlin.core.NameSpace
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable

typealias DataElementsArray = ArrayList<DataElementIdentifier>
typealias AuthorizedNameSpaces = ArrayList<NameSpace>
typealias AuthorizedDataElements = MutableMap<NameSpace, DataElementsArray>

class KeyAuthorizations(
    val authorizedNameSpaces: AuthorizedNameSpaces? = null,
    val authorizedDataElements: AuthorizedDataElements? = null
) : ICBORable {

    init {
        require(!authorizedNameSpaces.isNullOrEmpty() || !authorizedDataElements.isNullOrEmpty()) {
            "AuthorizedNameSpaces or AuthorizedDataElements should not be null or empty."
        }
    }

    override fun asCBOR(): CBORObject = CBORObject.NewMap().apply {
        authorizedNameSpaces?.let { Set(KEY_NAME_SPACES, it) }
        authorizedDataElements?.let { Set(KEY_DATA_ELEMENTS, it) }
    }

    companion object {

        internal const val KEY_NAME_SPACES = "nameSpaces"
        internal const val KEY_DATA_ELEMENTS = "dataElements"

        fun fromCBOR(cborObject: CBORObject): KeyAuthorizations {
            val authorizedNameSpaces: AuthorizedNameSpaces? = cborObject[KEY_NAME_SPACES]?.run {
                arrayListOf<NameSpace>().also {
                    values.forEach { value ->
                        it.add(value.AsString())
                    }
                }
            }

            val authorizedDataElements: AuthorizedDataElements? =
                cborObject[KEY_DATA_ELEMENTS]?.run {
                    mutableMapOf<NameSpace, DataElementsArray>().also {
                        entries.forEach { (key, value) ->
                            val dataElements = arrayListOf<DataElementIdentifier>()
                            value.values.forEach {
                                dataElements.add(it.AsString())
                            }
                            it[key.AsString()] = dataElements
                        }
                    }
                }

            return KeyAuthorizations(authorizedNameSpaces, authorizedDataElements)
        }
    }
}
