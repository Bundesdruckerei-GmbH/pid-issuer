/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.data

import de.bdr.statuslist.config.TransactionalOutsideBean
import de.bdr.statuslist.util.log
import java.io.Closeable
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

interface Storage {
    data class PoolConfig(val bits: Int, val size: Int, val creation: CreationStatus? = null)

    data class ListConfig(
        val bits: Int,
        val size: Int,
        val poolId: String,
        val listId: UUID,
        val version: Int = 1,
    )

    abstract class List {
        fun create(list: ListConfig, takeImmediately: Int? = null): kotlin.collections.List<Int> {
            check(takeImmediately == null || takeImmediately > 0) {
                "takeImmediately must be > 0 or null"
            }
            var generatedIndices = randomIndices(list.size)
            val immediatelyTaken =
                when (takeImmediately) {
                    null -> emptyList()
                    else -> {
                        val allGenerated = generatedIndices
                        val amountToTake = min(takeImmediately, allGenerated.size)
                        generatedIndices = generatedIndices.drop(amountToTake)
                        allGenerated.take(amountToTake)
                    }
                }
            create(generatedIndices, list, immediatelyTaken)

            return immediatelyTaken
        }

        protected abstract fun create(
            generatedIndices: kotlin.collections.List<Int>,
            list: ListConfig,
            immediatelyTaken: kotlin.collections.List<Int>,
        )

        /**
         * Sets a status on list identified by `uri` and `index` to `value`.
         *
         * @throws IndexOutOfBoundsException if the given index is out of the size of the list
         * @throws ValueOutOfRangeException if the given value is out of the range allowed for the
         *   list
         */
        fun updateStatus(index: Int, value: Int) {
            val list = config()

            if (index < 0 || index >= list.size) {
                throw IndexOutOfBoundsException()
            }

            if (value < 0 || value >= 1.shl(list.bits)) {
                throw ValueOutOfRangeException()
            }
            val offsetInList = calculateIndexOffset(list.bits, index)
            updateStatus(list, value, offsetInList)
        }

        protected abstract fun updateStatus(list: ListConfig, value: Int, offsetInList: Int)

        abstract fun config(allowCache: Boolean = true): ListConfig

        protected fun config(
            allowCache: Boolean,
            listUri: String,
            listConfigCache: ConcurrentHashMap<String, ListConfig>,
        ): ListConfig {
            return listConfigCache.compute(listUri) { _, cachedConfig ->
                if (allowCache && cachedConfig != null) return@compute cachedConfig
                return@compute readConfig()
            } ?: /* this will not happen */ error("No config returned")
        }

        protected abstract fun readConfig(): ListConfig

        abstract fun data(): ByteArray

        abstract fun poolId(): String

        abstract fun freeIndices(indices: kotlin.collections.List<Int>)

        abstract fun take(maxAmount: Int): kotlin.collections.List<Int>

        abstract fun isFull(): Boolean

        abstract fun isEmpty(): Boolean

        private val random = SecureRandom()

        private fun randomIndices(size: Int): kotlin.collections.List<Int> {
            val indices = ArrayList<Int>(size)
            for (index in 0 until size) {
                indices.add(index)
            }
            indices.shuffle(random)
            return indices
        }

        private fun calculateIndexOffset(bits: Int, index: Int): Int {
            val perByte = 8 / bits
            return (index / perByte) * 8 + 8 - bits - bits * (index % perByte)
        }
    }

    abstract class Pool(val poolName: String, val publicUrl: String) {
        abstract fun createOrVerifyPool(pool: PoolConfig): PoolConfig

        abstract fun config(): PoolConfig?

        /**
         * Creates a list in the pool.
         *
         * @param takeImmediately If set to a value != null, takes immediately this amount of
         *   elements from the list before it is registered in the pool.
         */
        @TransactionalOutsideBean
        open fun createList(
            takeImmediately: Int? = null
        ): Pair<String, kotlin.collections.List<Int>> {
            val pool = config() ?: error("pool $poolName not created")
            val listId = UUID.randomUUID()
            val listUri = "${publicUrl}/$listId"
            val immediateTaken =
                listOf(listUri)
                    .create(ListConfig(pool.bits, pool.size, poolName, listId), takeImmediately)
            if (immediateTaken.isNotEmpty()) {
                log.debug("Taken ${immediateTaken.size} from $listUri")
            }
            log.info("List $listUri created")

            return Pair(listUri, immediateTaken)
        }

        protected abstract fun listOf(listUri: String): List

        abstract fun currentLists(): kotlin.collections.List<String>

        abstract fun allListUris(): Iterable<String>

        abstract fun obtainPrecreationLock(): Closeable?
    }

    enum class CreationStatus {
        CREATING,
        DONE,
    }

    fun newListOf(listUri: String): List

    fun newPoolOf(poolId: String): Pool
}
