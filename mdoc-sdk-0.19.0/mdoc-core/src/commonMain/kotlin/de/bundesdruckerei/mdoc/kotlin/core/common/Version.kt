/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */

package de.bundesdruckerei.mdoc.kotlin.core.common

data class Version(
    val major: Int = 0,
    val minor: Int = 0,
    val patch: Int = 0
) : Comparable<Version> {

    val isValid = major >= 0 && minor >= 0 && patch >= 0

    override fun toString(): String = buildString {
        append("$major.$minor.$patch")
    }

    override fun compareTo(other: Version): Int = when {
        major != other.major -> major compareTo other.major
        minor != other.minor -> minor compareTo other.minor
        patch != other.patch -> patch compareTo other.patch
        else -> 0
    }

    fun nextMajor(): Version = Version(major + 1)
    fun nextMinor(): Version = Version(major, minor + 1)
    fun nextPatch(): Version = Version(major, minor, patch + 1)
}

internal val invalidVersion = Version(-1, -1, -1)

/**
 * Creates a Version from the string representation
 * (example "1.1.1")
 * according to the Semantic Versioning 2.0.0 specification.
 *
 * @throws  IllegalArgumentException
 *          If string does not conform to the Version as
 *          described in specification
 *
 */
fun String.toVersionOrNull(): Version? {
    val pattern = Regex("""(0|[1-9]\d*)?(?:\.)?(0|[1-9]\d*)?(?:\.)?(0|[1-9]\d*)?""")
    val result = pattern.matchEntire(this)
        ?: return null
    return Version(
        major = if (result.groupValues[1].isEmpty()) 0 else result.groupValues[1].toInt(),
        minor = if (result.groupValues[2].isEmpty()) 0 else result.groupValues[2].toInt(),
        patch = if (result.groupValues[3].isEmpty()) 0 else result.groupValues[3].toInt()
    )
}

/**
 * Return true, only if the MAJOR versions are different and therefore
 * incompatible API changes exist, otherwise false.
 */
fun Version.isApiIncompatible(with: Version): Boolean = major != with.major

