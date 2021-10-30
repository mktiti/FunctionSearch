package com.mktiti.fsearch.util

import arrow.core.Either
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ListSplitMapTest {

    @Test
    fun `test split map empty`() {
        val res = emptyList<String>().splitMap {
            when (val asNum = it.toIntOrNull()) {
                null -> Either.Right(it)
                else -> Either.Left(asNum)
            }
        }

        val expected = emptyList<Int>() to emptyList<String>()

        assertEquals(expected, res)
    }

    @Test
    fun `test split map simple`() {
        val res = listOf("asd", "123", "dsa", "321", "100").splitMap {
            when (val asNum = it.toIntOrNull()) {
                null -> Either.Right(it)
                else -> Either.Left(asNum)
            }
        }

        val expected = listOf(123, 321, 100) to listOf("asd", "dsa")

        assertEquals(expected, res)
    }

    @Test
    fun `test split map keep simple`() {
        val res = listOf("asd", "123", "dsa", "321", "100").splitMapKeep {
            it.toIntOrNull()
        }

        val expected = listOf(123, 321, 100) to listOf("asd", "dsa")

        assertEquals(expected, res)
    }

    @Test
    fun `test split map keep empty`() {
        val res = emptyList<String>().splitMapKeep {
            it.toIntOrNull()
        }

        val expected = emptyList<Int>() to emptyList<String>()

        assertEquals(expected, res)
    }

}