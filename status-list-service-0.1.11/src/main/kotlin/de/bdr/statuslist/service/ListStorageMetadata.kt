/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.time.Duration
import java.time.Instant
import java.util.UUID

class ListStorageMetadata(val listId: UUID, val version: Int, val expires: Instant) {

    companion object {

        private val objectMapper =
            ObjectMapper().registerKotlinModule().registerModules(JavaTimeModule())

        fun parse(value: String) = objectMapper.readValue<ListStorageMetadata>(value)
    }

    fun serialize() = objectMapper.writeValueAsString(this)

    fun isUpdated(version: Int) = version != this.version

    fun mayExpireAfterTwo(interval: Duration) =
        expires.isBefore(Instant.now().plus(interval.multipliedBy(2)))
}
