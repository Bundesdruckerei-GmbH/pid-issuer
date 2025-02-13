/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.query

import assertk.Assert
import assertk.assertions.support.expected
import java.lang.StringBuilder

/** Custom assertion for better error messages */
fun Assert<Set<Map<String, CredentialIdAndDisclosures>>>.isCorrectResolutionResult(
    expected: Set<Map<String, CredentialIdAndDisclosures>>
) = given { actual ->
    val queryIdsWithCredentialIdsAndAssociatedCredentialIdAndDisclosures =
        actual
            .flatMap { it.entries.map { entry -> Pair(entry.key, entry.value) } }
            .groupBy { Pair(it.first, it.second.id) }
    queryIdsWithCredentialIdsAndAssociatedCredentialIdAndDisclosures.forEach {
        if (it.value.size > 1) {
            val first = it.value.first()
            it.value.forEach { other ->
                if (other.second.disclosed != first.second.disclosed) {
                    expected(
                        "Each association of a credential id to a query id must lead to exactly the same disclosures. Found a mismatch for ${first.first} = ${first.second.id}. Disclosures ${other.second.disclosed} vs. ${first.second.disclosed}"
                    )
                }
            }
        }
    }

    val actualQueryIdsWithCredentialIdsWithDuplicates =
        actual.map { it.mapValues { entry -> entry.value.id } }
    val actualQueryIdsWithCredentialIds = actualQueryIdsWithCredentialIdsWithDuplicates.toSet()

    val nonUnique =
        actualQueryIdsWithCredentialIds
            .associateWith { current ->
                actualQueryIdsWithCredentialIdsWithDuplicates.count { it == current }
            }
            .filterValues { it > 1 }
    if (nonUnique.isNotEmpty()) {
        expected(
            "distinct associations of query ids to credentials, but got the following non-unique associations: ${nonUnique.keys}"
        )
    }

    val expectedQueryIdsWithCredentialIds =
        expected.map { it.mapValues { entry -> entry.value.id } }.toSet()
    val unexpected = actualQueryIdsWithCredentialIds.minus(expectedQueryIdsWithCredentialIds)
    val missing = expectedQueryIdsWithCredentialIds.minus(actualQueryIdsWithCredentialIds)

    if (missing.isNotEmpty() || unexpected.isNotEmpty()) {
        val result = StringBuilder("correct resolution result, but ")
        var hasMissing = false
        if (missing.isNotEmpty()) {
            hasMissing = true
            result.append("missing $missing")
        }
        if (unexpected.isNotEmpty()) {
            if (hasMissing) result.append(" and ")
            result.append("unexpected $unexpected")
        }
        expected(result.toString())
    }

    // TODO match disclosures
}
