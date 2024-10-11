/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.common

enum class DataKey(val key: String) {
    VERSION("version"),
    DOC_TYPE("docType"),
    NAME_SPACES("nameSpaces"),
    REQUEST_INFO("requestInfo"),
    ITEMS_REQUEST("itemsRequest"),
    READER_AUTH("readerAuth"),
    DOC_REQUESTS("docRequests"),
    MAC_KEYS("macKeys"),
}
