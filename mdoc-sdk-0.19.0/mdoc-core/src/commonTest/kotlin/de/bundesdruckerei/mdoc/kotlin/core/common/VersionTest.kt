/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */

package de.bundesdruckerei.mdoc.kotlin.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VersionTest {
    @Test
    fun `is initialization valid`() {
        Version(1, 2, 3)
    }

    @Test
    fun `parse valid version`() {
        val actual = "1.2.3".toVersionOrNull()
        val expected = Version(1, 2, 3)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `parse incomplete version missing minor and patch`() {
        val actual = "123".toVersionOrNull()
        val expected = Version(123)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `parse incomplete version missing patch`() {
        val actual = "21.222".toVersionOrNull()
        val expected = Version(21, 222)
        println(expected)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `parse incomplete version missing minor`() {
        val actual = "4..555".toVersionOrNull()
        val expected = Version(4, 0, 555)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `parse invalid version`() {
        assertThat("1.3.4.5.42424663".toVersionOrNull()).isNull()
    }

    @Test
    fun `parse invalid version with negative number`() {
        assertThat("-1.3.-4.5.42424663".toVersionOrNull()).isNull()
    }

    @Test
    fun `check if version is valid`() {
        val actual = Version(1, 1, 1).isValid
        val expected = true
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `check if version is invalid`() {
        val actual = Version(-1, 1, 1).isValid
        val expected = false
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `convert version to string`() {
        val version = Version(1, 0, 2)
        assertThat(version.toString()).isEqualTo("1.0.2")
    }

    @Test
    fun `compare same versions`() {
        val version1 = Version(1, 0, 2)
        val version2 = Version(1, 0, 2)
        assertThat(version1.compareTo(version2)).isEqualTo(0)
    }

    @Test
    fun `compare increased major version`() {
        val version1 = Version(1, 0, 0)
        val version2 = Version(2, 0, 0)
        assertThat(version1.compareTo(version2)).isEqualTo(-1)
    }

    @Test
    fun `compare increased minor version`() {
        val version1 = Version(1, 0, 0)
        val version2 = Version(1, 1, 0)
        assertThat(version1.compareTo(version2)).isEqualTo(-1)
    }

    @Test
    fun `compare increased patch version`() {
        val version1 = Version(1, 1, 5)
        val version2 = Version(1, 1, 4)
        assertThat(version1.compareTo(version2)).isEqualTo(1)
    }

    @Test
    fun `compare decreased major version`() {
        val version1 = Version(2, 0, 0)
        val version2 = Version(1, 0, 0)
        assertThat(version1.compareTo(version2)).isEqualTo(1)
    }

    @Test
    fun `compare decreased minor version`() {
        val version1 = Version(2, 2, 0)
        val version2 = Version(2, 0, 0)
        assertThat(version1.compareTo(version2)).isEqualTo(1)
    }

    @Test
    fun `compare decreased patch version`() {
        val version1 = Version(3, 0, 0)
        val version2 = Version(3, 0, 2)
        assertThat(version1.compareTo(version2)).isEqualTo(-1)
    }

    @Test
    fun `create next major version`() {
        val newMajor = Version(2, 3, 5).nextMajor()
        assertThat(newMajor.major).isEqualTo(3)
        assertThat(newMajor.minor).isEqualTo(0)
        assertThat(newMajor.patch).isEqualTo(0)
    }

    @Test
    fun `given version list is ordered by latest`() {
        val versionList =
            listOf(
                Version(1),
                Version(2),
                Version(4),
                Version(3)
            )

        val sortedList = versionList.sortedDescending()

        assertThat(sortedList[0].major).isEqualTo(4)
        assertThat(sortedList[1].major).isEqualTo(3)
        assertThat(sortedList[2].major).isEqualTo(2)
        assertThat(sortedList[3].major).isEqualTo(1)
    }

    @Test
    fun `create next minor version`() {
        val newMinor = Version(2, 3, 5).nextMinor()
        assertThat(newMinor.major).isEqualTo(2)
        assertThat(newMinor.minor).isEqualTo(4)
        assertThat(newMinor.patch).isEqualTo(0)
    }

    @Test
    fun `create next patch version`() {
        val newPatch = Version(2, 3, 5).nextPatch()
        assertThat(newPatch.major).isEqualTo(2)
        assertThat(newPatch.minor).isEqualTo(3)
        assertThat(newPatch.patch).isEqualTo(6)
    }

    @Test
    fun `major version changed and api is incompatible`() {
        val version = Version(2, 3, 5)
        assertThat(version.isApiIncompatible(version.nextMajor())).isTrue()
        assertThat(version.isApiIncompatible(version.nextPatch())).isFalse()
        assertThat(version.isApiIncompatible(version.nextMinor())).isFalse()
    }
}