/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.data

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.matches
import de.bdr.statuslist.config.AppConfiguration
import de.bdr.statuslist.stats.StatsService
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PostgreSqlStorageTest {
    @MockK private val config: AppConfiguration = mockk()
    @MockK private val statsService: StatsService = mockk()
    @MockK private val poolAdapter: PoolAdapter = mockk()
    @MockK private val listAdapter: ListAdapter = mockk()
    @MockK private val listIndexAdapter: ListIndexAdapter = mockk()

    private val storage =
        PostgreSqlStorage(config, poolAdapter, listAdapter, listIndexAdapter, statsService)

    @BeforeEach
    fun setUp() {
        every { statsService.listCreated(any(), any(), any()) } just Runs
        every { statsService.indicesTaken(any(), any(), any()) } just Runs
        every { statsService.statusUpdated(any(), any()) } just Runs
        every { config.publicUrl } returns "http://test.example"
        every { poolAdapter.createPool(any(), any(), any()) } just Runs
        every { listAdapter.createList(any(), any(), any(), any(), any(), any(), any()) } just Runs
        every { listIndexAdapter.saveIndices(any(), any()) } just Runs
        every { listAdapter.updateDataAndVersion(any(), any(), any(), any(), any()) } just Runs
    }

    @Test
    fun `should create pool by provided pool config`() {
        val pool = storage.newPoolOf("test-pool")
        every { poolAdapter.findPoolAttributesByName("test-pool") } returns null
        every { poolAdapter.existsByName("test-pool") } returns false

        val creationConfig = pool.createOrVerifyPool(Storage.PoolConfig(1, 16))

        with(creationConfig) {
            assertThat(bits).isEqualTo(1)
            assertThat(size).isEqualTo(16)
            assertThat(creation).isEqualTo(Storage.CreationStatus.DONE)
        }
        verify { poolAdapter.createPool(1, 16, "test-pool") }
    }

    @Test
    fun `should provide pool config by existing pool`() {
        val pool = storage.newPoolOf("test-pool")
        every { poolAdapter.findPoolAttributesByName("test-pool") } returns
            PoolAdapter.PoolAttributes(1, 2, 32, "test-pool")
        every { poolAdapter.existsByName("test-pool") } returns true

        val creationConfig = pool.createOrVerifyPool(Storage.PoolConfig(1, 16))

        with(creationConfig) {
            assertThat(bits).isEqualTo(2)
            assertThat(size).isEqualTo(32)
            assertThat(creation).isEqualTo(Storage.CreationStatus.DONE)
        }
        verify(exactly = 0) { poolAdapter.createPool(any(), any(), any()) }
    }

    @Test
    fun `should create new list`() {
        val uuid = UUID.randomUUID()
        val listUri = "http://test.example/$uuid"
        val list = storage.newListOf(listUri)
        every { poolAdapter.findPoolIdByName("test-pool") } returns 1

        val indices = list.create(Storage.ListConfig(1, 16, "test-pool", uuid))

        assertThat(indices).hasSize(0)
        verify {
            listAdapter.createList(uuid, 1, "http://test.example/", 1, 16, 1, "0000000000000000")
        }
        verify { listIndexAdapter.saveIndices(match { it.size == 16 }, eq(uuid)) }
        verify { statsService.listCreated("test-pool", listUri, 16) }
    }

    @Test
    fun `should create list and immediately take all`() {
        val uuid = UUID.randomUUID()
        val listUri = "http://test.example/$uuid"
        val list = storage.newListOf(listUri)
        every { poolAdapter.findPoolIdByName("test-pool") } returns 1

        val indices = list.create(Storage.ListConfig(1, 16, "test-pool", uuid), 16)

        assertThat(indices).hasSize(16)
        verify {
            listAdapter.createList(uuid, 1, "http://test.example/", 1, 16, 1, "0000000000000000")
        }
        verify(exactly = 0) { listIndexAdapter.saveIndices(any(), any()) }
        verify { statsService.listCreated("test-pool", listUri, 16) }
        verify { statsService.indicesTaken("test-pool", listUri, 16) }
    }

    @Test
    fun `should create list and immediately take some`() {
        val uuid = UUID.randomUUID()
        val listUri = "http://test.example/$uuid"
        val list = storage.newListOf(listUri)
        every { poolAdapter.findPoolIdByName("test-pool") } returns 1

        val indices = list.create(Storage.ListConfig(1, 16, "test-pool", uuid), 8)

        assertThat(indices).hasSize(8)
        verify {
            listAdapter.createList(uuid, 1, "http://test.example/", 1, 16, 1, "0000000000000000")
        }
        verify { listIndexAdapter.saveIndices(match { it.size == 8 }, eq(uuid)) }
        verify { statsService.listCreated("test-pool", listUri, 16) }
        verify { statsService.indicesTaken("test-pool", listUri, 8) }
    }

    @Test
    fun `should create list and immediately take not more than size`() {
        val uuid = UUID.randomUUID()
        val listUri = "http://test.example/$uuid"
        val list = storage.newListOf(listUri)
        every { poolAdapter.findPoolIdByName("test-pool") } returns 1

        val indices = list.create(Storage.ListConfig(1, 16, "test-pool", uuid), 32)

        assertThat(indices).hasSize(16)
        verify {
            listAdapter.createList(uuid, 1, "http://test.example/", 1, 16, 1, "0000000000000000")
        }
        verify(exactly = 0) { listIndexAdapter.saveIndices(any(), any()) }
        verify { statsService.listCreated("test-pool", listUri, 16) }
        verify { statsService.indicesTaken("test-pool", listUri, 16) }
    }

    @Test
    fun `should create new list via pool`() {
        val pool = storage.newPoolOf("test-pool")
        every { poolAdapter.findPoolAttributesByName("test-pool") } returns
            PoolAdapter.PoolAttributes(1, 2, 32, "test-pool")
        every { poolAdapter.findPoolIdByName("test-pool") } returns 1

        val indices = pool.createList()

        with(indices) {
            assertThat(first).all {
                matches("http://test\\.example/.{36}".toRegex())
                transform { UUID.fromString(it.takeLast(36)) }.isInstanceOf(UUID::class)
            }
            assertThat(second).hasSize(0)
        }
        verify { listAdapter.createList(any(), any(), any(), any(), any(), any(), any()) }
        verify { statsService.listCreated(any(), any(), any()) }
    }

    @Test
    fun `should create new list via pool take some`() {
        val pool = storage.newPoolOf("test-pool")
        every { poolAdapter.findPoolAttributesByName("test-pool") } returns
            PoolAdapter.PoolAttributes(1, 2, 32, "test-pool")
        every { poolAdapter.findPoolIdByName("test-pool") } returns 1

        val indices = pool.createList(16)

        with(indices) {
            assertThat(first).all {
                matches("http://test\\.example/.{36}".toRegex())
                transform { UUID.fromString(it.takeLast(36)) }.isInstanceOf(UUID::class)
            }
            assertThat(second).hasSize(16)
        }
        verify { listAdapter.createList(any(), any(), any(), any(), any(), any(), any()) }
        verify { listIndexAdapter.saveIndices(match { it.size == 16 }, any()) }
        verify { statsService.listCreated(any(), any(), any()) }
    }

    @Test
    fun `should update status`() {
        val uuid = UUID.randomUUID()
        val listUri = "http://test.example/$uuid"
        val list = storage.newListOf(listUri)
        every { listAdapter.findListAttributesById(uuid) } returns
            ListAdapter.ListAttributes(uuid, "test-pool", "http://test.example/", 2, 16, 1)

        list.updateStatus(12, 1)

        verify { listAdapter.updateDataAndVersion(uuid, 1, 31, 2, 1) }
        verify { statsService.statusUpdated("test-pool", listUri) }
    }

    @Test
    fun `should throw exception on index out of bounds`() {
        val uuid = UUID.randomUUID()
        val list = storage.newListOf("http://test.example/$uuid")
        every { listAdapter.findListAttributesById(uuid) } returns
            ListAdapter.ListAttributes(uuid, "test-pool", "http://test.example/", 2, 16, 1)

        assertFailure { list.updateStatus(16, 1) }.isInstanceOf(IndexOutOfBoundsException::class)
    }

    @Test
    fun `should throw exception on value out of range`() {
        val uuid = UUID.randomUUID()
        val list = storage.newListOf("http://test.example/$uuid")
        every { listAdapter.findListAttributesById(uuid) } returns
            ListAdapter.ListAttributes(uuid, "test-pool", "http://test.example/", 2, 16, 1)

        assertFailure { list.updateStatus(12, 4) }.isInstanceOf(ValueOutOfRangeException::class)
    }

    @Test
    fun `should return bitString(8) as byteArray`() {
        val uuid = UUID.randomUUID()
        val list = storage.newListOf("http://test.example/$uuid")

        mapOf("00000000" to 0, "00000001" to 1, "00000010" to 2, "10000001" to -127).forEach {
            every { listAdapter.findDataById(uuid) } returns it.key

            val data = list.data()

            assertThat(data).all {
                hasSize(1)
                containsExactly(it.value.toByte())
            }
        }
    }

    @Test
    fun `should return bitString(16) as byteArray`() {
        val uuid = UUID.randomUUID()
        val list = storage.newListOf("http://test.example/$uuid")
        every { listAdapter.findDataById(uuid) } returns "0000001000000001"

        val data = list.data()

        assertThat(data).all {
            hasSize(2)
            containsExactly(2.toByte(), 1.toByte())
        }
    }
}
