/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.data

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.cause
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.ninjasquad.springmockk.SpykBean
import de.bdr.statuslist.config.AppConfiguration
import de.bdr.statuslist.config.PostgreSqlConfiguration
import de.bdr.statuslist.config.TransactionalAop
import de.bdr.statuslist.stats.LoggingStatsService
import io.mockk.Runs
import io.mockk.andThenJust
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.dao.CannotAcquireLockException
import org.springframework.retry.support.RetryTemplateBuilder
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate

@DataJdbcTest(properties = ["spring.profiles.include=docker"])
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "postgres")
@Import(
    PostgreSqlStorage::class,
    PoolAdapter::class,
    ListAdapter::class,
    ListIndexAdapter::class,
    LoggingStatsService::class,
    PostgreSqlConfiguration::class,
    TransactionalAop::class,
)
@EnableConfigurationProperties(AppConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostgreSqlStorageIT {

    @Autowired private lateinit var transactionTemplate: TransactionTemplate

    @Autowired private lateinit var storage: PostgreSqlStorage

    @SpykBean private lateinit var listAdapter: ListAdapter

    @SpykBean private lateinit var listIndexAdapter: ListIndexAdapter

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        transactionTemplate.isolationLevel = Isolation.DEFAULT.value()
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `should verify that only one transaction can obtain precreation lock`() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse()

        val pool = storage.newPoolOf("test")
        pool.createOrVerifyPool(Storage.PoolConfig(1, 128))

        val lock = ReentrantLock()
        val condition = lock.newCondition()
        var t1LockObtained: Boolean? = null
        var t2LockObtained: Boolean? = null

        val t1 = thread {
            lock.withLock {
                condition.await() // wait for t2 to obtain precreation lock
                transactionTemplate.execute {
                    val precreationLock = pool.obtainPrecreationLock()
                    t1LockObtained = precreationLock != null
                }
                condition.signal()
            }
        }
        val t2 = thread {
            lock.withLock {
                transactionTemplate.execute {
                    val precreationLock = pool.obtainPrecreationLock()
                    t2LockObtained = precreationLock != null
                    condition.signal()
                    condition.await() // keeps the precreation lock until t1 has finished
                }
            }
        }

        t1.join(1000)
        t2.join(1000)
        assertThat(t1LockObtained).isNotNull().isFalse()
        assertThat(t2LockObtained).isNotNull().isTrue()
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `should trigger serialization error`() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse()

        val originalRetryTemplate = ReflectionTestUtils.getField(storage, "retryTemplate")
        ReflectionTestUtils.setField(
            storage,
            "retryTemplate",
            RetryTemplateBuilder()
                .maxAttempts(1)
                .retryOn(PSQLException::class.java)
                .traversingCauses()
                .build(),
        )

        val list = initList()
        val lock = ReentrantLock()
        val condition = lock.newCondition()
        var throwable: Throwable? = null
        transactionTemplate.isolationLevel = Isolation.REPEATABLE_READ.value()

        val t1 = thread {
            transactionTemplate.execute {
                lock.withLock { condition.await() }
                list.updateStatus(1, 1)
                lock.withLock { condition.signal() }
            }
        }
        val t2 = thread {
            transactionTemplate.execute {
                list.updateStatus(2, 1)
                lock.withLock {
                    condition.signal()
                    condition.await(200, TimeUnit.MILLISECONDS)
                }
            }
        }

        t1.setUncaughtExceptionHandler { _, e -> throwable = e }

        t1.join(1000)
        t2.join(1000)

        assertThat(throwable).isNotNull().cause().isNotNull().isInstanceOf(PSQLException::class)
        throwable?.printStackTrace()

        ReflectionTestUtils.setField(storage, "retryTemplate", originalRetryTemplate)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `should retry on serialization error`() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse()

        val list = initList()

        every { listAdapter.updateDataAndVersion(any(), any(), any(), any(), any()) }
            .throws(
                CannotAcquireLockException("", PSQLException("", PSQLState.SERIALIZATION_FAILURE))
            )
            .andThenJust(Runs)

        list.updateStatus(3, 1)

        verify(exactly = 2) { listAdapter.updateDataAndVersion(any(), any(), any(), any(), any()) }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `should verify that transaction is rolled back on exception`() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse()

        val pool = storage.newPoolOf("test")
        pool.createOrVerifyPool(Storage.PoolConfig(1, 128))
        val initialSize = pool.allListUris().toList().size

        every { listIndexAdapter.saveIndices(any(), any()) }.throws(IllegalArgumentException())

        assertFailure { pool.createList() }.isInstanceOf(IllegalArgumentException::class)
        assertThat(pool.allListUris().toList().size).isEqualTo(initialSize)
    }

    private fun initList(): Storage.List {
        val pool = storage.newPoolOf("test")
        pool.createOrVerifyPool(Storage.PoolConfig(1, 128))
        if (pool.currentLists().isEmpty()) {
            pool.createList()
        }
        return storage.newListOf(pool.currentLists().first())
    }
}
