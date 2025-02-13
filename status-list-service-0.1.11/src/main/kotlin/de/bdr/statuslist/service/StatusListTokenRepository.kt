/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.service

import java.io.Closeable
import java.time.Instant
import java.util.UUID

interface StatusListTokenStorage {
    fun obtainPoolLock(poolId: String): Closeable?

    fun metadata(listId: UUID): ListStorageMetadata

    fun storeMetadata(metadata: ListStorageMetadata)

    fun store(listId: UUID, tokenFormat: TokenFormat, serialized: ByteArray)
}

interface StatusListTokenSource {
    fun load(listId: UUID, tokenFormat: TokenFormat): TokenData?

    fun lastModified(listId: UUID, tokenFormat: TokenFormat): Instant
}

class TokenData(val data: ByteArray, val lastModified: Instant)

interface StatusListTokenRepository : StatusListTokenStorage, StatusListTokenSource
