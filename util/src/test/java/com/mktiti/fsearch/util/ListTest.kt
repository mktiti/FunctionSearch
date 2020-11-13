package com.mktiti.fsearch.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ListTest {

    @Test
    fun `test permutations empty`() {
        val res = emptyList<String>().allPermutations()
        assertEquals(listOf(emptyList<String>()), res)
    }

    @Test
    fun `test permutations singleton`() {
        val res = listOf("Only").allPermutations()
        assertEquals(listOf(listOf("Only")), res)
    }

    @Test
    fun `test permutations numbers`() {
        val list = (0 until 3).toList()
        val res = list.allPermutations()

        val expected = listOf(
                listOf(0, 1, 2),
                listOf(0, 2, 1),

                listOf(1, 0, 2),
                listOf(1, 2, 0),

                listOf(2, 0, 1),
                listOf(2, 1, 0)
        )

        assertEquals(expected, res)
    }

}