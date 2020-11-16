package com.mktiti.fsearch.maven.util

import com.mktiti.fsearch.modules.ArtifactId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class DependencyUtilTest {

    @Test
    fun `test parse shortest type`() {
        val result = DependencyUtil.parseListedArtifact("com.mygroup:my.artifact:jar:1.0.0:compile")
        val expected = ArtifactId(
                group = listOf("com", "mygroup"),
                name = "my.artifact",
                version = "1.0.0"
        )

        assertEquals(expected, result)
    }

    @Test
    fun `test parse javadoc`() {
        val result = DependencyUtil.parseListedArtifact("com.mygroup:my.artifact:jar:javadoc:1.0.0:compile")
        val expected = ArtifactId(
                group = listOf("com", "mygroup"),
                name = "my.artifact",
                version = "1.0.0"
        )

        assertEquals(expected, result)
    }

    @Test
    fun `test parse dashed version`() {
        val result = DependencyUtil.parseListedArtifact("com.google.guava:guava:jar:30.0-jre:compile")
        val expected = ArtifactId(
                group = listOf("com", "google", "guava"),
                name = "guava",
                version = "30.0-jre"
        )

        assertEquals(expected, result)
    }

    @Test
    fun `test parse dashed version javadoc`() {
        val result = DependencyUtil.parseListedArtifact("com.google.guava:guava:jar:javadoc:30.0-jre:compile")
        val expected = ArtifactId(
                group = listOf("com", "google", "guava"),
                name = "guava",
                version = "30.0-jre"
        )

        assertEquals(expected, result)
    }

    @Test
    fun `test parse dashed name`() {
        val result = DependencyUtil.parseListedArtifact("org.apache.commons:commons-lang3:jar:3.11:compile")
        val expected = ArtifactId(
                group = listOf("org", "apache", "commons"),
                name = "commons-lang3",
                version = "3.11"
        )

        assertEquals(expected, result)
    }

    @Test
    fun `test parse dashed name javadoc`() {
        val result = DependencyUtil.parseListedArtifact("org.apache.commons:commons-lang3:jar:javadoc:3.11:compile")
        val expected = ArtifactId(
                group = listOf("org", "apache", "commons"),
                name = "commons-lang3",
                version = "3.11"
        )

        assertEquals(expected, result)
    }

}