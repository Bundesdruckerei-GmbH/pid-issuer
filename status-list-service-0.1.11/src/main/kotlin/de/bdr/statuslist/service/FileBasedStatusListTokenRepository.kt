/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.service

import de.bdr.statuslist.config.AppConfiguration
import de.bdr.statuslist.util.log
import java.io.Closeable
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class FileBasedStatusListTokenRepository(val config: AppConfiguration) : StatusListTokenRepository {

    override fun obtainPoolLock(poolId: String): Closeable? {
        try {
            val poolLockfile = StorageFiles.poolLockfile(config, poolId)
            Files.createFile(poolLockfile)
            return Closeable { Files.deleteIfExists(poolLockfile) }
        } catch (e: FileAlreadyExistsException) {
            log.debug("lock is already claimed", e)
            return null
        }
    }

    override fun metadata(listId: UUID): ListStorageMetadata {
        val file = StorageFiles.listStorageMetadata(config, listId)
        return try {
            ListStorageMetadata.parse(Files.readString(file))
        } catch (e: NoSuchFileException) {
            ListStorageMetadata(listId, 0, Instant.now())
        }
    }

    override fun storeMetadata(metadata: ListStorageMetadata) {
        Files.writeString(
            StorageFiles.listStorageMetadata(config, metadata.listId),
            metadata.serialize(),
        )
    }

    override fun store(listId: UUID, tokenFormat: TokenFormat, serialized: ByteArray) {
        val tmp = StorageFiles.statusListTmp(config, tokenFormat, listId)
        val destination = StorageFiles.statusList(config, tokenFormat, listId)
        Files.write(tmp, serialized)
        Files.move(tmp, destination, ATOMIC_MOVE)
    }

    override fun load(listId: UUID, tokenFormat: TokenFormat): TokenData? {
        return try {
            val file = file(listId, tokenFormat)
            val created =
                Files.readAttributes(file, BasicFileAttributes::class.java)
                    .lastModifiedTime()
                    .toInstant()
            TokenData(Files.readAllBytes(file), created)
        } catch (e: NoSuchFileException) {
            null
        }
    }

    override fun lastModified(listId: UUID, tokenFormat: TokenFormat): Instant {
        return Files.readAttributes(file(listId, tokenFormat), BasicFileAttributes::class.java)
            .lastModifiedTime()
            .toInstant()
    }

    private fun file(listId: UUID, format: TokenFormat) =
        config.storageDirectory.resolve("${format.name.lowercase()}/$listId")
}
