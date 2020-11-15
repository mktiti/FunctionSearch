package com.mktiti.fsearch.maven.util

import com.mktiti.fsearch.modules.ArtifactId
import org.junit.jupiter.api.Test
import org.xmlunit.builder.DiffBuilder
import java.io.ByteArrayOutputStream
import kotlin.test.assertFalse

internal class MockPomHandlerTest {

    @Test
    fun `test pom template single dependency`() {
        val testArtifact = ArtifactId.parse("test-group.subgroup:test-artifact:test-version")!!

        val result = with(ByteArrayOutputStream()) {
            MockPomHandler.createMockPom(listOf(testArtifact), this)
            toString("utf-8")
        }

        val expected = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
            	<modelVersion>4.0.0</modelVersion>
            	<groupId>fsearch-dependency-fetch-mock</groupId>
            	<artifactId>fsearch-dependency-fetch-mock</artifactId>
            	<packaging>jar</packaging>
            	<version>1.0-SNAPSHOT</version>
            	<dependencies>
            		<dependency>
            			<groupId>test-group.subgroup</groupId>
            			<artifactId>test-artifact</artifactId>
            			<version>test-version</version>
            		</dependency>
            	</dependencies>
            </project>
        """.trim()

        val diff = DiffBuilder.compare(expected)
                .withTest(result)
                .ignoreWhitespace()
                .ignoreComments()
                .build()

        println(">>> Expected:")
        println(expected)
        println("============================")
        println(">>> Result:")
        println(result)

        assertFalse(diff.hasDifferences(), diff.toString())
    }

    @Test
    fun `test pom template multiple dependencies`() {
        val testArtifacts = (0 until 5).map {
            ArtifactId.parse("test-group.subgroup.sub$it:test-artifact-$it:test-version-$it")!!
        }

        val result = with(ByteArrayOutputStream()) {
            MockPomHandler.createMockPom(testArtifacts, this)
            toString("utf-8")
        }

        val expected = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
            	<modelVersion>4.0.0</modelVersion>
            	<groupId>fsearch-dependency-fetch-mock</groupId>
            	<artifactId>fsearch-dependency-fetch-mock</artifactId>
            	<packaging>jar</packaging>
            	<version>1.0-SNAPSHOT</version>
            	<dependencies>
            		<dependency>
            			<groupId>test-group.subgroup.sub0</groupId>
            			<artifactId>test-artifact-0</artifactId>
            			<version>test-version-0</version>
            		</dependency>
                    <dependency>
            			<groupId>test-group.subgroup.sub1</groupId>
            			<artifactId>test-artifact-1</artifactId>
            			<version>test-version-1</version>
            		</dependency>
                    <dependency>
            			<groupId>test-group.subgroup.sub2</groupId>
            			<artifactId>test-artifact-2</artifactId>
            			<version>test-version-2</version>
            		</dependency>
                    <dependency>
            			<groupId>test-group.subgroup.sub3</groupId>
            			<artifactId>test-artifact-3</artifactId>
            			<version>test-version-3</version>
            		</dependency>
                    <dependency>
            			<groupId>test-group.subgroup.sub4</groupId>
            			<artifactId>test-artifact-4</artifactId>
            			<version>test-version-4</version>
            		</dependency>
            	</dependencies>
            </project>
        """.trim()

        val diff = DiffBuilder.compare(expected)
                .withTest(result)
                .ignoreWhitespace()
                .ignoreComments()
                .build()

        println(">>> Expected:")
        println(expected)
        println("============================")
        println(">>> Result:")
        println(result)
        assertFalse(diff.hasDifferences(), diff.toString())
    }

}