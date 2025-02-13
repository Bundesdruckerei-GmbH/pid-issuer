/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.ssi.statuslist

import de.bdr.openid4vc.statuslist.StatusList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class StatusListTest {

    @Test
    fun `create status list with bit size 1 and set test values`() {
        val statusList = StatusList(16, 1)
        statusList.set(0, 1)
        statusList.set(1, 0)
        statusList.set(2, 0)
        statusList.set(3, 1)
        statusList.set(4, 1)
        statusList.set(5, 1)
        statusList.set(6, 0)
        statusList.set(7, 1)
        statusList.set(8, 1)
        statusList.set(9, 1)
        statusList.set(10, 0)
        statusList.set(11, 0)
        statusList.set(12, 0)
        statusList.set(13, 1)
        statusList.set(14, 0)
        statusList.set(15, 1)

        assert(statusList.getList().contentEquals(byteArrayOf(0xB9.toByte(), 0xA3.toByte())))

        println("test:" + statusList.getEncoded())

        assert(statusList.getEncoded() == "eNrbuRgAAhcBXQ")
    }

    @Test
    fun `create status list with bit size 1, default values and set test values`() {
        val statusList = StatusList(16, 1, 0x00)
        statusList.set(0, 1)
        statusList.set(3, 1)
        statusList.set(4, 1)
        statusList.set(5, 1)
        statusList.set(7, 1)
        statusList.set(8, 1)
        statusList.set(9, 1)
        statusList.set(13, 1)
        statusList.set(15, 1)

        assert(statusList.getList().contentEquals(byteArrayOf(0xB9.toByte(), 0xA3.toByte())))

        println("test:" + statusList.getEncoded())

        assert(statusList.getEncoded() == "eNrbuRgAAhcBXQ")
    }

    @Test
    fun `create status list with bit size 1 and pre-initialized list`() {
        val statusList = StatusList(16, 1, list = byteArrayOf(0xB9.toByte(), 0xA3.toByte()))

        assert(statusList.getList().contentEquals(byteArrayOf(0xB9.toByte(), 0xA3.toByte())))

        println("test:" + statusList.getEncoded())

        assert(statusList.getEncoded() == "eNrbuRgAAhcBXQ")
    }

    @Test
    fun `create status list and set test values bit size 2`() {
        val statusList = StatusList(12, 2)
        statusList.set(0, 1)
        statusList.set(1, 2)
        statusList.set(2, 0)
        statusList.set(3, 3)
        statusList.set(4, 0)
        statusList.set(5, 1)
        statusList.set(6, 0)
        statusList.set(7, 1)
        statusList.set(8, 1)
        statusList.set(9, 2)
        statusList.set(10, 3)
        statusList.set(11, 3)

        assert(
            statusList
                .getList()
                .contentEquals(byteArrayOf(0xC9.toByte(), 0x44.toByte(), 0xF9.toByte()))
        )

        println("test:" + statusList.getEncoded())

        assert(statusList.getEncoded() == "eNo76fITAAPfAgc")
    }

    @Test
    fun `create status list and set test values bit size 4`() {
        val statusList = StatusList(12, 4)
        statusList.set(0, 0)
        statusList.set(1, 2)
        statusList.set(2, 1)
        statusList.set(3, 7)
        statusList.set(4, 0)
        statusList.set(5, 14)
        statusList.set(6, 0)
        statusList.set(7, 8)
        statusList.set(8, 1)
        statusList.set(9, 1)
        statusList.set(10, 15)
        statusList.set(11, 10)

        assert(
            statusList
                .getList()
                .contentEquals(
                    byteArrayOf(0x20, 0x71, 0xe0.toByte(), 0x80.toByte(), 0x11, 0xaf.toByte())
                )
        )

        println("test:" + statusList.getEncoded())

        assert(statusList.getEncoded() == "eNpTKHzQILgeAAjMArI")
    }

    @Test
    fun `create status list and set test values bit size 8`() {
        val statusList = StatusList(12, 8)
        statusList.set(0, 0)
        statusList.set(1, 2)
        statusList.set(2, 12)
        statusList.set(3, 45)
        statusList.set(4, 37)
        statusList.set(5, 199.toByte())
        statusList.set(6, 254.toByte())
        statusList.set(7, 1)
        statusList.set(8, 0)
        statusList.set(9, 0)
        statusList.set(10, 211.toByte())
        statusList.set(11, 98)

        assert(
            statusList
                .getList()
                .contentEquals(
                    byteArrayOf(
                        0,
                        2,
                        12,
                        45,
                        37,
                        199.toByte(),
                        254.toByte(),
                        1,
                        0,
                        0,
                        211.toByte(),
                        98
                    )
                )
        )

        println("test:" + statusList.getEncoded())

        assert(statusList.getEncoded() == "eNpjYOLRVT3-j5GB4XISABDJA1w")
    }

    @Test
    fun `test for invalid constructor`() {
        // invalid size
        assertThrows<IllegalArgumentException> { StatusList(0, 1) }
        assertThrows<IllegalArgumentException> { StatusList(-1, 1) }

        // invalid bits
        assertThrows<IllegalArgumentException> { StatusList(1, 0) }
        assertThrows<IllegalArgumentException> { StatusList(1, -1) }
        assertThrows<IllegalArgumentException> { StatusList(1, 3) }

        // size does not fit byte
        assertThrows<IllegalArgumentException> { StatusList(7, 1) }
        assertThrows<IllegalArgumentException> { StatusList(6, 2) }

        // pre-initialized list does not fit size
        assertThrows<IllegalArgumentException> { StatusList(8, 1, list = ByteArray(2)) }
        assertThrows<IllegalArgumentException> { StatusList(8, 2, list = ByteArray(3)) }

        // set default value and pre-initialized list
        assertThrows<IllegalArgumentException> { StatusList(8, 1, 0x01, ByteArray(1)) }
    }

    @Test
    fun `test for set to invalid index`() {
        val statusList = StatusList(12, 2)
        assertThrows<IllegalArgumentException> { statusList.set(-1, 0) }
        assertThrows<IllegalArgumentException> { statusList.set(12, 0) }
    }

    @Test
    fun `test for set to invalid value`() {
        val statusList = StatusList(16, 1)
        assertThrows<IllegalArgumentException> { statusList.set(0, -1) }
        assertThrows<IllegalArgumentException> { statusList.set(0, 2) }

        val statusList2 = StatusList(12, 2)
        assertThrows<IllegalArgumentException> { statusList2.set(0, 4) }

        val statusList3 = StatusList(12, 4)
        assertThrows<IllegalArgumentException> { statusList3.set(0, 16) }
    }
}
