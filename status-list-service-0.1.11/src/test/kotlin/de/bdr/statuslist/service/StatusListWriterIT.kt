/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.profiles.include=docker"],
)
class StatusListWriterIT {
    @MockkBean(relaxed = true)
    lateinit var statusListTokenStorage: FileBasedStatusListTokenRepository
    @Autowired lateinit var statusListWriter: StatusListWriter

    @Test
    fun `should write status list`() {
        // Given
        val uuid = UUID.randomUUID()
        val version = 88
        val now = Instant.now()
        every { statusListTokenStorage.metadata(any()) } returns
            ListStorageMetadata(uuid, version, now)

        // When
        statusListWriter.writePoolTokens("verified-email")

        // Then
        verify { statusListTokenStorage.metadata(any()) }
        verify { statusListTokenStorage.storeMetadata(any()) }
    }
}
