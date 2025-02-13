/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class ForBeginners {
    @Test
    fun test1() {
        val b: UByte = 1U
        val s: UShort = 123U
        val i = 4711U
        val l = 123456UL
        println("$b\n$s\n$i\n$l")
    }

    @Test
    fun test2() {
        val a = 0b1100
        val b = 0b1010
        println(a and b)
        println(a or b)
        println(a xor b)
        println(a.inv())
        println(a shl 1)
        println(a shr 1)
    }

    @Test
    fun test3() {
        var a: Any = "abc"
        println(a)
        a = 88
        println(a)
        a = 123.45
        println(a)
    }

    @Test
    fun test4() {
        val arrayOfNumbers = Array(5, { (it + 1) * 2 })
        for (element in arrayOfNumbers) {
            println(element) // 2, 4, 6, 8, 10
        }
        val b = BooleanArray(3, { it % 2 == 0 })
        b.indices.forEach { println("$it: " + b[it]) }
        Assertions.assertFalse(isValid(""))
    }

    fun isValid(input: String): Boolean {
        return input.isNotEmpty()
    }

    fun addition(a: Int, b: Int): Int = a + b

    val additionLambda: (Int, Int) -> Int = { a, b -> a + b }
    val additionUnamed = fun(a: Int, b: Int): Int = a + b
    val additionUnamedLambda = { a: Int, b: Int -> a + b }
}
