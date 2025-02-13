/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.data

import java.util.UUID
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("app.storage-type", havingValue = "postgres")
class ListAdapter(private val jdbcTemplate: JdbcTemplate) {
    fun createList(
        id: UUID,
        poolId: Long,
        baseUri: String,
        bits: Int,
        size: Int,
        version: Int,
        data: String,
    ) {
        jdbcTemplate.update(
            "INSERT INTO lists(id, pool_id, base_uri, bits, size, version, data) VALUES (?, ?, ?, ?, ?, ?, cast(? AS varbit))",
            id,
            poolId,
            baseUri,
            bits,
            size,
            version,
            data,
        )
    }

    fun findDataById(listId: UUID): String? {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT data FROM lists WHERE id = ?",
                String::class.java,
                listId,
            )
        } catch (_: EmptyResultDataAccessException) {
            null
        }
    }

    fun findSizeById(listId: UUID): Int? {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT size FROM lists WHERE id = ?",
                Int::class.java,
                listId,
            )
        } catch (_: EmptyResultDataAccessException) {
            null
        }
    }

    fun findListAttributesById(listId: UUID): ListAttributes? {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT l.id, p.name, l.base_uri, l.bits, l.size, l.version FROM lists l JOIN pools p on p.id = l.pool_id WHERE l.id = ?",
                ListAttributes.indexMapper,
                listId,
            )
        } catch (_: EmptyResultDataAccessException) {
            null
        }
    }

    fun findAllListUris(poolId: Long): List<String> {
        return jdbcTemplate
            .queryForList("SELECT base_uri, id FROM lists WHERE pool_id = ?", poolId)
            .map { it["base_uri"] as String + it["id"] as UUID }
    }

    fun findAllListUrisWithIndices(poolId: Long): List<String> {
        return jdbcTemplate
            .queryForList(
                "SELECT l.base_uri, l.id FROM lists l JOIN list_indices li on l.id = li.list_id WHERE pool_id = ? GROUP BY l.id ORDER BY count(l.id)",
                poolId,
            )
            .map { it["base_uri"] as String + it["id"] as UUID }
    }

    fun updateDataAndVersion(
        listId: UUID,
        value: Int,
        fromIndex: Int,
        bits: Int,
        versionDelta: Int,
    ) {
        jdbcTemplate.update(
            "UPDATE lists SET data = overlay(data PLACING cast(? AS varbit) FROM ?), version = version + ? WHERE id = ?",
            value.toString(2).padStart(bits, '0'),
            fromIndex,
            versionDelta,
            listId,
        )
    }

    class ListAttributes(
        val id: UUID,
        val poolName: String,
        val baseUri: String,
        val bits: Int,
        val size: Int,
        val version: Int,
    ) {
        companion object {
            val indexMapper: RowMapper<ListAttributes> = RowMapper { rs, _ ->
                ListAttributes(
                    rs.getObject("id", UUID::class.java),
                    rs.getString("name"),
                    rs.getString("base_uri"),
                    rs.getInt("bits"),
                    rs.getInt("size"),
                    rs.getInt("version"),
                )
            }
        }
    }
}
