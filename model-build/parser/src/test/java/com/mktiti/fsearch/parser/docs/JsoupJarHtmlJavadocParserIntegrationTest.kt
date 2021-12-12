package com.mktiti.fsearch.parser.docs

import com.mktiti.fsearch.core.javadoc.FunctionDoc
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import com.mktiti.fsearch.model.build.serialize.ArtifactDocSerializer
import com.mktiti.fsearch.model.build.service.JarHtmlJavadocParser
import org.junit.jupiter.api.Tag
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Tag("integrationTest")
class JsoupJarHtmlJavadocParserIntegrationTest {

    data class TestCase(
            val type: String,
            val dir: String,
            val jar: String,
            val expected: String
    )

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun tests(): List<TestCase> = listOf(
                TestCase("Apache Commons Lang", "/docs/apache-commons/", "commons-lang3-3.11-javadoc.jar", "expected-docs.jsonl"),
                TestCase("Google Guava", "/docs/guava/", "guava-30.0-jre-javadoc.jar", "expected-docs.jsonl")
        )

        private fun loadExpected(dir: Path): FunDocMap = ArtifactDocSerializer.readFromDir(dir, "expected")

        private fun FunctionDoc.clean(): FunctionDoc = FunctionDoc(
                link, paramNames, shortInfo,
                longInfo = longInfo
                        ?.replace("\\s".toRegex(), "")
                        ?.replace("\u00A0", "") // NBSP
        )

    }

    private val parser: JarHtmlJavadocParser = JsoupJarHtmlJavadocParser(MapJavaInfoRepo)

    @ParameterizedTest
    @MethodSource("tests")
    fun `test case`(case: TestCase) {
        val basePath = JsoupJarHtmlJavadocParserIntegrationTest::class.java.getResource(case.dir).path
        val jarPath = Paths.get(basePath, case.jar)

        val resultDocs = assertNotNull(parser.parseJar(jarPath)).convertMap()
        val expected = loadExpected(Paths.get(basePath)).convertMap()

        expected.forEach { (info, expectedDocs) ->
            val parsedDocs = assertNotNull(resultDocs[info], "Docs not found for fun $info")

            assertEquals(expectedDocs.clean(), parsedDocs.clean())
        }
    }

}

















