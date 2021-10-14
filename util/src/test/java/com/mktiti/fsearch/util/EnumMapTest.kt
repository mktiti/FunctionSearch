package com.mktiti.fsearch.util

import com.mktiti.fsearch.util.EnumMapTest.Person.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

internal class EnumMapTest {

    private enum class Person(val capName: String) {
        ALICE("Alice"), BOB("Bob"), CHARLIE("Charlie")
    }

    @Test
    fun `test eager`() {
        val map = EnumMap.eager<Person, String> {
            when (it) {
                ALICE -> "Alice"
                BOB -> "Bob"
                CHARLIE -> "Charlie"
            }
        }

        values().forEach {
            assertEquals(it.capName, map[it])
        }
    }

    @Test
    fun `test lazy`() {
        val map = EnumMap.lazy<Person, String> {
            when (it) {
                ALICE -> "Alice"
                BOB -> "Bob"
                CHARLIE -> "Charlie"
            }
        }

        values().forEach {
            assertEquals(it.capName, map[it])
        }
    }

    @Test
    fun `test unsafe success`() {
        val completeBackingMap = mapOf(
                ALICE to "Alice",
                BOB to "Bob",
                CHARLIE to "Charlie"
        )

        val map = EnumMap.unsafe(completeBackingMap)

        values().forEach {
            assertEquals(it.capName, map[it])
        }
    }

    @Test
    fun `test unsafe fail`() {
        val completeBackingMap = mapOf(
                ALICE to "Alice",
                BOB to "Bob"
        )

        val map = EnumMap.unsafe(completeBackingMap)

        assertEquals(ALICE.capName, map[ALICE])
        assertEquals(BOB.capName, map[BOB])
        assertFailsWith<NoSuchElementException> { map[CHARLIE] }
    }

}