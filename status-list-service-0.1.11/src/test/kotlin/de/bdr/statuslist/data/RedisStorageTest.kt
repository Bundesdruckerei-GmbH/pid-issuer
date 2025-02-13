/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.data

import assertk.all
import assertk.assertFailure
import assertk.assertThat
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
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.ListOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.ValueOperations

class RedisStorageTest {
    @MockK private val config: AppConfiguration = mockk()
    @MockK private val redis: RedisTemplate<String, String> = mockk()
    @MockK private val byteRedis: RedisTemplate<String, ByteArray> = mockk()
    @MockK private val statsService: StatsService = mockk()
    @MockK private val hashOperations: HashOperations<String, String, String> = mockk()
    @MockK private val listOperations: ListOperations<String, String> = mockk()
    @MockK private val valueOperations: ValueOperations<String, String> = mockk()
    @MockK private val setOperations: SetOperations<String, String> = mockk()

    private val storage = RedisStorage(config, redis, byteRedis, statsService)

    @BeforeEach
    fun setUp() {
        every { redis.opsForList() } returns listOperations
        every { redis.opsForValue() } returns valueOperations
        every { redis.opsForHash<String, String>() } returns hashOperations
        every { redis.opsForSet() } returns setOperations
        every { hashOperations.putIfAbsent(any(), any(), any()) } returns true
        every { hashOperations.putAll(any(), any()) } just Runs
        every { listOperations.rightPushAll(any(), any<List<String>>()) } returns null
        every { listOperations.rightPush(any(), any()) } returns null
        every { setOperations.add(any(), any()) } returns null
        every { valueOperations.bitField(any(), any()) } returns null
        every { statsService.listCreated(any(), any(), any()) } just Runs
        every { statsService.indicesTaken(any(), any(), any()) } just Runs
        every { statsService.statusUpdated(any(), any()) } just Runs
        every { config.publicUrl } returns "http://test.example"
    }

    @Test
    fun `should create pool by provided pool config`() {
        val pool = storage.newPoolOf("test-pool")
        every { hashOperations.entries(eq("pool:config:test-pool")) } returns emptyMap()
        val creationConfig = pool.createOrVerifyPool(Storage.PoolConfig(1, 16))

        with(creationConfig) {
            assertThat(bits).isEqualTo(1)
            assertThat(size).isEqualTo(16)
            assertThat(creation).isEqualTo(Storage.CreationStatus.DONE)
        }
        verify { hashOperations.putIfAbsent(eq("pool:config:test-pool"), any(), any()) }
        verify { hashOperations.putAll(eq("pool:config:test-pool"), any()) }
    }

    @Test
    fun `should provide pool config by existing pool`() {
        val pool = storage.newPoolOf("test-pool")
        every { hashOperations.entries(eq("pool:config:test-pool")) } returns
            mapOf("bits" to "2", "size" to "32", "creation" to "DONE")
        val creationConfig = pool.createOrVerifyPool(Storage.PoolConfig(1, 16))

        with(creationConfig) {
            assertThat(bits).isEqualTo(2)
            assertThat(size).isEqualTo(32)
            assertThat(creation).isEqualTo(Storage.CreationStatus.DONE)
        }
        verify(exactly = 0) { hashOperations.putIfAbsent(any(), any(), any()) }
        verify(exactly = 0) { hashOperations.putAll(any(), any()) }
    }

    @Test
    fun `should create new list`() {
        val uuid = UUID.randomUUID()
        val listUri = "http://test.example/$uuid"
        val list = storage.newListOf(listUri)
        val indices = list.create(Storage.ListConfig(1, 16, "test-pool", uuid))

        assertThat(indices).hasSize(0)
        verify { listOperations.rightPushAll(eq("list:indices:$listUri"), any<List<String>>()) }
        verify { valueOperations.bitField(eq("list:data:$listUri"), any()) }
        verify { hashOperations.putAll(eq("list:config:$listUri"), any()) }
        verify { setOperations.add("pool:lists:all:test-pool", listUri) }
        verify { listOperations.rightPush("pool:lists:current:test-pool", listUri) }
        verify { statsService.listCreated("test-pool", listUri, 16) }
    }

    @Test
    fun `should create list and immediately take all`() {
        val uuid = UUID.randomUUID()
        val listUri = "http://test.example/$uuid"
        val list = storage.newListOf(listUri)
        val indices = list.create(Storage.ListConfig(1, 16, "test-pool", uuid), 16)

        assertThat(indices).hasSize(16)
        verify { valueOperations.bitField(eq("list:data:$listUri"), any()) }
        verify { hashOperations.putAll(eq("list:config:$listUri"), any()) }
        verify { setOperations.add("pool:lists:all:test-pool", listUri) }
        verify { statsService.listCreated("test-pool", listUri, 16) }
        verify { statsService.indicesTaken("test-pool", listUri, 16) }
    }

    @Test
    fun `should create list and immediately take some`() {
        val uuid = UUID.randomUUID()
        val listUri = "http://test.example/$uuid"
        val list = storage.newListOf(listUri)
        val indices = list.create(Storage.ListConfig(1, 16, "test-pool", uuid), 8)

        assertThat(indices).hasSize(8)
        verify { listOperations.rightPushAll(eq("list:indices:$listUri"), any<List<String>>()) }
        verify { valueOperations.bitField(eq("list:data:$listUri"), any()) }
        verify { hashOperations.putAll(eq("list:config:$listUri"), any()) }
        verify { setOperations.add("pool:lists:all:test-pool", listUri) }
        verify { listOperations.rightPush("pool:lists:current:test-pool", listUri) }
        verify { statsService.listCreated("test-pool", listUri, 16) }
        verify { statsService.indicesTaken("test-pool", listUri, 8) }
    }

    @Test
    fun `should create list and immediately take not more than size`() {
        val uuid = UUID.randomUUID()
        val listUri = "http://test.example/$uuid"
        val list = storage.newListOf(listUri)
        val indices = list.create(Storage.ListConfig(1, 16, "test-pool", uuid), 32)

        assertThat(indices).hasSize(16)
        verify { valueOperations.bitField(eq("list:data:$listUri"), any()) }
        verify { hashOperations.putAll(eq("list:config:$listUri"), any()) }
        verify { setOperations.add("pool:lists:all:test-pool", listUri) }
        verify { statsService.listCreated("test-pool", listUri, 16) }
        verify { statsService.indicesTaken("test-pool", listUri, 16) }
    }

    @Test
    fun `should create new list via pool`() {
        val pool = storage.newPoolOf("test-pool")
        every { hashOperations.entries(eq("pool:config:test-pool")) } returns
            mapOf("bits" to "1", "size" to "32", "creation" to "DONE")

        val indices = pool.createList()

        with(indices) {
            assertThat(first).all {
                matches("http://test\\.example/.{36}".toRegex())
                transform { UUID.fromString(it.takeLast(36)) }.isInstanceOf(UUID::class)
            }
            assertThat(second).hasSize(0)
        }
        verify { setOperations.add("pool:lists:all:test-pool", indices.first) }
        verify { statsService.listCreated(any(), any(), any()) }
    }

    @Test
    fun `should create new list via pool take some`() {
        val pool = storage.newPoolOf("test-pool")
        every { hashOperations.entries(eq("pool:config:test-pool")) } returns
            mapOf("bits" to "1", "size" to "32", "creation" to "DONE")

        val indices = pool.createList(16)

        with(indices) {
            assertThat(first).all {
                matches("http://test\\.example/.{36}".toRegex())
                transform { UUID.fromString(it.takeLast(36)) }.isInstanceOf(UUID::class)
            }
            assertThat(second).hasSize(16)
        }
        verify {
            listOperations.rightPushAll(
                match { it.startsWith("list:indices:") },
                any<List<String>>(),
            )
        }
        verify { setOperations.add("pool:lists:all:test-pool", indices.first) }
        verify { statsService.listCreated(any(), any(), any()) }
        verify { statsService.indicesTaken(any(), any(), 16) }
    }

    @Test
    fun `should update status`() {
        val uuid = UUID.randomUUID()
        val listUri = "http://test.example/$uuid"
        val list = storage.newListOf(listUri)
        every { hashOperations.entries("list:config:$listUri") } returns
            mapOf(
                "bits" to "2",
                "size" to "16",
                "poolId" to "test-pool",
                "listId" to "$uuid",
                "version" to "1",
            )
        every { hashOperations.increment(any(), any(), any<Long>()) } returns 1
        list.updateStatus(12, 1)

        verify { valueOperations.bitField(eq("list:data:$listUri"), any()) }
        verify { hashOperations.increment("list:config:$listUri", "version", 1) }
        verify { statsService.statusUpdated("test-pool", listUri) }
    }

    @Test
    fun `should throw exception on index out of bounds`() {
        val uuid = UUID.randomUUID()
        val listUri = "http://test.example/$uuid"
        val list = storage.newListOf(listUri)
        every { hashOperations.entries("list:config:$listUri") } returns
            mapOf(
                "bits" to "2",
                "size" to "16",
                "poolId" to "test-pool",
                "listId" to "$uuid",
                "version" to "1",
            )
        assertFailure { list.updateStatus(16, 1) }.isInstanceOf(IndexOutOfBoundsException::class)
    }

    @Test
    fun `should throw exception on value out of range`() {
        val uuid = UUID.randomUUID()
        val listUri = "http://test.example/$uuid"
        val list = storage.newListOf(listUri)
        every { hashOperations.entries("list:config:$listUri") } returns
            mapOf(
                "bits" to "2",
                "size" to "16",
                "poolId" to "test-pool",
                "listId" to "$uuid",
                "version" to "1",
            )
        assertFailure { list.updateStatus(12, 4) }.isInstanceOf(ValueOutOfRangeException::class)
    }
}
