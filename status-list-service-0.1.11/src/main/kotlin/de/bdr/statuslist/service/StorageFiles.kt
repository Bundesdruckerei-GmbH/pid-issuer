/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.service

import de.bdr.statuslist.config.AppConfiguration
import java.nio.file.Path
import java.util.UUID

object StorageFiles {

    fun statusListTokenDirectories(config: AppConfiguration): Iterable<Path> =
        listOf(
            config.storageDirectory.resolve("jwt"),
            config.storageDirectory.resolve("cwt"),
            config.storageDirectory.resolve("json"),
            config.storageDirectory.resolve("cbor"),
        )

    fun poolLockfile(config: AppConfiguration, poolId: String) =
        config.storageDirectory.resolve("$poolId.lock")

    fun statusList(config: AppConfiguration, format: TokenFormat, listId: UUID) =
        config.storageDirectory.resolve("${format.name.lowercase()}/$listId")

    fun statusListTmp(config: AppConfiguration, format: TokenFormat, listId: UUID) =
        config.storageDirectory.resolve("$listId.${format.name.lowercase()}.tmp")

    fun listStorageMetadata(config: AppConfiguration, listId: UUID) =
        config.storageDirectory.resolve("$listId.metadata")
}
