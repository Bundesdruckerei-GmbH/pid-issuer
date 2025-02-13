/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.query

import de.bdr.openid4vc.common.credentials.Credential
import de.bdr.openid4vc.common.credentials.IssuerInfo
import de.bdr.openid4vc.common.credentials.StatusInfo
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredential
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialFormat
import de.bdr.openid4vc.common.vp.dcql.ArrayElementSelector
import de.bdr.openid4vc.common.vp.dcql.DcqlQuery
import de.bdr.openid4vc.common.vp.dcql.DistinctClaimsPathPointer
import de.bdr.openid4vc.common.vp.dcql.DistinctClaimsPathPointer.Companion.ROOT
import java.time.Instant
import jsonStringContent
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream

fun loadQueryCredentialsAndExpectedResult(
    name: String
): Triple<DcqlQuery, Set<Credential>, ExpectedResult> {
    val resource = "/testdata/vp/query-tests/dcql/$name.json"
    val json =
        Json.decodeFromStream<JsonObject>(
            DcqlQueryTest::class.java.getResourceAsStream(resource) ?: error("Missing resource")
        )

    val dcqlQuery = Json.decodeFromJsonElement<DcqlQuery>(json["query"] ?: error("Missing query"))

    val credentials =
        Json.decodeFromJsonElement<Set<TestCredential>>(
            json["credentials"] ?: error("Missing credentials")
        )

    if (credentials.groupBy { it.id }.values.map { it.size }.any { it != 1 }) {
        error("Duplicate id in credential list")
    }

    val expectedResult =
        Json.decodeFromJsonElement<Set<Map<String, CredentialIdAndDisclosures>>>(
            json["expected"] ?: error("Missing expected result")
        )

    return Triple(dcqlQuery, credentials, expectedResult)
}

fun loadCorrectResultTestCases(
    dcqlQueryTestCase: DcqlQueryTestCase
): Sequence<CorrectResultTestCase> {
    val (query, credentials, expected) = dcqlQueryTestCase.queryCredentialsAndExpectedResult
    return expected
        .asSequence()
        .map {
            it.mapValues { entry ->
                val credential =
                    credentials.find { credential ->
                        (credential as TestCredential).id == entry.value.id
                    }
                (credential as TestCredential).applyDisclosures(entry.value.disclosed)
            }
        }
        .map { CorrectResultTestCase("${dcqlQueryTestCase.name} / $it", query, it) }
}

typealias ExpectedResult = Set<Map<String, CredentialIdAndDisclosures>>

@Serializable
class CredentialIdAndDisclosures(val id: String, val disclosed: Set<DistinctClaimsPathPointer>) {

    override fun toString(): String {
        return "{id=$id, disclosed=[${disclosed.joinToString()}]}"
    }

    override fun hashCode(): Int {
        var hash = 709664531
        hash = hash * 31 + id.hashCode()
        hash = hash * 31 + disclosed.hashCode()
        return hash
    }

    override fun equals(other: Any?) =
        other === this || other is CredentialIdAndDisclosures && equals(other)

    private fun equals(other: CredentialIdAndDisclosures) =
        id == other.id && disclosed == other.disclosed
}

@Serializable(with = TestCredentialSerializer::class)
sealed class TestCredential : Credential {
    abstract val id: String

    /**
     * Constructs a new credential instance from this credential with the given disclosures applied.
     * This means, that only these disclosures will remain in the result. All other SD claims will
     * be gone from `claims` and `discloseable`.
     */
    abstract fun applyDisclosures(disclosures: Set<DistinctClaimsPathPointer>): TestCredential
}

object TestCredentialSerializer :
    JsonContentPolymorphicSerializer<TestCredential>(TestCredential::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<TestCredential> {
        val type = (element as? JsonObject)?.get("type")?.jsonStringContent()
        return when (type) {
            SdJwtVcCredentialFormat.format -> SdJwtVcTestCredential.serializer()
            else -> error("No serializer for type $type")
        }
    }
}

@Serializable
class SdJwtVcTestCredential(override val id: String, val type: String, val data: JsonObject) :
    TestCredential(), SdJwtVcCredential {
    @Transient override val format = SdJwtVcCredentialFormat

    @Transient override val issuer = IssuerInfo("undefined")

    @Transient override val status: StatusInfo? = null

    override fun withStatus(status: StatusInfo): SdJwtVcCredential {
        error("Not implemented")
    }

    override fun withIssuer(issuer: IssuerInfo): SdJwtVcCredential {
        error("Not implemented")
    }

    @Transient override val vct: String = data["vct"]?.jsonStringContent() ?: error("Missing vct")

    @Transient override val issuedAt: Instant? = null

    @Transient override val validFrom: Instant? = null

    @Transient override val expiresAt: Instant? = null

    @Transient
    private var internalClaims: JsonObject =
        data["claims"] as? JsonObject ?: error("Missing claims")

    @Transient
    override val claims: JsonObject
        get() = internalClaims

    override fun claims(toDisclose: Set<DistinctClaimsPathPointer>): JsonObject {
        TODO("Not yet implemented")
    }

    @Transient
    private var internalDiscloseable =
        (data["discloseable"] as? JsonArray)?.let {
            Json.decodeFromJsonElement<Set<DistinctClaimsPathPointer>>(it)
        } ?: error("Missing discloseable")

    override fun toString(): String {
        return "{id=$id, disclosures=[${discloseable.joinToString()}]}"
    }

    @Transient
    override val discloseable: Set<DistinctClaimsPathPointer>
        get() = internalDiscloseable

    override fun applyDisclosures(
        disclosures: Set<DistinctClaimsPathPointer>
    ): SdJwtVcTestCredential {
        val copy = SdJwtVcTestCredential(id, type, data)
        val removedArrayElements = mutableSetOf<DistinctClaimsPathPointer>()
        copy.internalClaims =
            claims.removeDisclosures(ROOT, discloseable.minus(disclosures), removedArrayElements)
                ?: error("Discloseable must not contain ROOT")
        copy.internalDiscloseable = disclosures.shiftArrayIndices(removedArrayElements)
        return copy
    }
}

/**
 * Removing array elements by applying the disclosures requires that the remaining discloseable set
 * is corrected because the higher array element indices are no longer correct.
 *
 * **Example** Credential with claims /array/0, /array/1 and /array/2, each as a discloseable.
 * /array/1 is removed. The new discloseable list must be /array/0, /array/1 and not /array/0,
 * /array/2.
 */
private fun Set<DistinctClaimsPathPointer>.shiftArrayIndices(
    removedArrayElements: MutableSet<DistinctClaimsPathPointer>
): Set<DistinctClaimsPathPointer> {
    val removedArrayElementsSorted = removedArrayElements.toMutableList()
    removedArrayElementsSorted.sortWith(HighArrayIndicesBeforeLowOnesAndLongPathsBeforeShortPaths)
    var result = asSequence()
    removedArrayElementsSorted.forEach { removedArrayElement ->
        result =
            result.map { element ->
                if (
                    element.isChildOf(
                        removedArrayElement.parent()
                            ?: error("Removed array element must not be root")
                    )
                ) {
                    val elementIndex =
                        (element.selectors.get(removedArrayElement.selectors.size - 1)
                                as ArrayElementSelector)
                            .index
                    val removedIndex =
                        (removedArrayElement.selectors.last() as ArrayElementSelector).index
                    if (removedIndex < elementIndex) {
                        val newArrayElementSelector = ArrayElementSelector(elementIndex - 1)
                        val newSelectors = element.selectors.toMutableList()
                        newSelectors[removedArrayElement.selectors.size - 1] =
                            newArrayElementSelector
                        DistinctClaimsPathPointer(newSelectors)
                    } else {
                        element
                    }
                } else {
                    element
                }
            }
    }
    return result.toSet()
}

object HighArrayIndicesBeforeLowOnesAndLongPathsBeforeShortPaths :
    Comparator<DistinctClaimsPathPointer> {
    override fun compare(a: DistinctClaimsPathPointer, b: DistinctClaimsPathPointer): Int {
        return if (a.selectors.size < b.selectors.size) {
            1
        } else if (a.selectors.size > b.selectors.size) {
            -1
        } else {
            if (a.parent() == b.parent() && a.selectors.last() is ArrayElementSelector) {
                val aIndex = (a.selectors.last() as ArrayElementSelector).index
                val bIndex = (b.selectors.last() as ArrayElementSelector).index
                if (aIndex < bIndex) {
                    1
                } else if (aIndex > bIndex) {
                    -1
                } else {
                    0
                }
            } else {
                0
            }
        }
    }
}

private fun JsonObject.removeDisclosures(
    current: DistinctClaimsPathPointer,
    disclosuresToRemove: Set<DistinctClaimsPathPointer>,
    removedArrayElements: MutableSet<DistinctClaimsPathPointer>,
): JsonObject? {
    if (disclosuresToRemove.contains(current)) {
        return null
    }
    val result = mutableMapOf<String, JsonElement>()
    forEach { name, value ->
        val elementPath = current.objectElement(name)
        val newValue =
            when (value) {
                is JsonObject ->
                    value.removeDisclosures(elementPath, disclosuresToRemove, removedArrayElements)
                is JsonArray ->
                    value.removeDisclosures(elementPath, disclosuresToRemove, removedArrayElements)
                else -> value.removeDisclosures(elementPath, disclosuresToRemove)
            }
        if (newValue != null) {
            result[name] = newValue
        }
    }
    return JsonObject(result)
}

private fun JsonArray.removeDisclosures(
    current: DistinctClaimsPathPointer,
    disclosuresToRemove: Set<DistinctClaimsPathPointer>,
    removedArrayElements: MutableSet<DistinctClaimsPathPointer>,
): JsonArray? {
    if (disclosuresToRemove.contains(current)) {
        return null
    }
    val result = mutableListOf<JsonElement>()
    forEachIndexed { index, value ->
        val elementPath = current.arrayElement(index)
        val newValue =
            when (value) {
                is JsonObject ->
                    value.removeDisclosures(elementPath, disclosuresToRemove, removedArrayElements)
                is JsonArray ->
                    value.removeDisclosures(elementPath, disclosuresToRemove, removedArrayElements)
                else -> value.removeDisclosures(elementPath, disclosuresToRemove)
            }
        if (newValue == null) {
            removedArrayElements.add(elementPath)
        } else {
            result.add(newValue)
        }
    }
    return JsonArray(result)
}

private fun JsonElement.removeDisclosures(
    current: DistinctClaimsPathPointer,
    disclosuresToRemove: Set<DistinctClaimsPathPointer>,
): JsonElement? = if (disclosuresToRemove.contains(current)) null else this
