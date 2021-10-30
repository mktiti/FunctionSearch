package com.mktiti.fsearch.maven.util

import com.mktiti.fsearch.modules.ArtifactId
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class TgfUtilTest {

    @Test
    fun `test simple graph`() {
        // A──>B──>C──>D
        // │           ^
        // └───────────┘

        val lines = listOf(
                "1 group-a:art-a:jar:ver-a",
                "2 group-b:art-b:jar:ver-b:scope-b",
                "3 group-c:art-c:jar:ver-c:scope-c",
                "4 group-d:art-d:jar:ver-d:scope-d",
                "#", // separator
                "1 2 a-to-b",
                "2 3 b-to-c",
                "3 4 c-to-d",
                "1 4 a-to-d",
        )

        val result = parseDependencyTgfGraph(lines)

        val artA = ArtifactId(listOf("group-a"), "art-a", "ver-a")
        val artB = ArtifactId(listOf("group-b"), "art-b", "ver-b")
        val artC = ArtifactId(listOf("group-c"), "art-c", "ver-c")
        val artD = ArtifactId(listOf("group-d"), "art-d", "ver-d")
        
        val expected = mapOf(
                artA to listOf(ArtifactDependency(artB, "a-to-b"), ArtifactDependency(artD, "a-to-d")),
                artB to listOf(ArtifactDependency(artC, "b-to-c")),
                artC to listOf(ArtifactDependency(artD, "c-to-d")),
                artD to emptyList()
        )

        assertEquals(expected, result)
    }

}