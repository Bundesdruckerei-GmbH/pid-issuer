/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.data

import java.util.UUID
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("app.storage-type", havingValue = "postgres")
class ListIndexAdapter(private val jdbcTemplate: JdbcTemplate) {
    fun saveIndices(indices: List<Int>, listId: UUID) {
        val batch = indices.map { arrayOf(it, listId) }
        jdbcTemplate.batchUpdate("INSERT INTO list_indices(index, list_id) VALUES (?,?)", batch)
    }

    fun takeIndices(listId: UUID, maxAmount: Int): List<Int> {
        return jdbcTemplate.query(
            "DELETE FROM list_indices WHERE id IN (SELECT id FROM list_indices WHERE list_id = ? ORDER BY id LIMIT ?) RETURNING index",
            { rs, _ -> rs.getInt("index") },
            listId,
            maxAmount,
        )
    }

    fun countIndices(listId: UUID): Int {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM list_indices WHERE list_id = ?",
            Int::class.java,
            listId,
        )
    }
}
