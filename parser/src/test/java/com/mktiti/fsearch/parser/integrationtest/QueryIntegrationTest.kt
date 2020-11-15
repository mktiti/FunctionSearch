package com.mktiti.fsearch.parser.integrationtest

import com.mktiti.fsearch.core.fit.FunIdParam
import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.fit.JavaQueryFitter
import com.mktiti.fsearch.core.fit.QueryFitter
import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.parser.function.DirectoryFunctionCollector
import com.mktiti.fsearch.parser.query.AntlrQueryParser
import com.mktiti.fsearch.parser.query.QueryParser
import com.mktiti.fsearch.parser.type.DirectoryInfoCollector
import com.mktiti.fsearch.util.cutLast
import com.mktiti.fsearch.util.orElse
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.test.assertEquals

@Tag("integrationTest")
class QueryIntegrationTest {

    companion object {
        data class VerificationEntry(
                val query: String,
                val results: Set<FunctionInfo>
        )

        private fun parseEntry(queryLine: String, resultLine: String) = VerificationEntry(
                query = queryLine,
                results = when (val trimmedRes = resultLine.trim()) {
                    "-" -> emptySet()
                    else -> {
                        trimmedRes.split(';').map { result ->
                            val (fileAndName, sig) = result.split('(')
                            val (file, name) = fileAndName.split(regex = "\\.|(::)".toRegex()).cutLast()
                            val (filePackage, fileName) = file.cutLast()
                            val isStatic = fileAndName.contains("::")

                            val params = sig.dropLast(1).split(',').map { param ->
                                PrimitiveType.fromNameSafe(param)?.let {
                                    FunIdParam.Type(MapJavaInfoRepo.primitive(it))
                                }.orElse {
                                    val (pack, type) = param.split('.').cutLast()
                                    FunIdParam.Type(MinimalInfo(pack, type))
                                }
                            }

                            FunctionInfo(
                                    file = MinimalInfo(filePackage, fileName),
                                    name = name,
                                    isStatic = isStatic,
                                    paramTypes = params
                            )
                        }.toSet()
                    }
                }
        )

        private fun testResource(resource: String): List<VerificationEntry> {
            return QueryIntegrationTest::class.java.getResource(resource).readText().lines().map {
                it.trimStart()
            }.filter {
                !it.startsWith("#") && it.isNotEmpty()
            }.chunked(2).map { (q, r) ->
                parseEntry(q, r)
            }
        }

        fun verify(dataRes: String, codeBase: String) {
            val codebasePath = Paths.get(QueryIntegrationTest::class.java.getResource(codeBase).path)
            val infoRepo: JavaInfoRepo = MapJavaInfoRepo
            val (javaRepo, typeResolver, functions) = CompilerUtil.withCompiled(codebasePath) { path ->
                val (javaRepo, baseResolver) = RepoTestUtil.minimalRepos(infoRepo)

                val (directs, templates) = DirectoryInfoCollector(infoRepo).collectInitial(path)
                val typeRepo = RadixTypeRepo(
                        directs = directs,
                        templates = templates
                )

                println("Loaded direct types: " + typeRepo.allTypes)
                println("Loaded type templates: " + typeRepo.allTemplates)

                val typeResolver = FallbackResolver(typeRepo, baseResolver)
                val functions = DirectoryFunctionCollector.collectFunctions(path, javaRepo, infoRepo, typeResolver)

                Triple(javaRepo, typeResolver, functions)
            }

            val queryParser: QueryParser = AntlrQueryParser(javaRepo, infoRepo, typeResolver)

            testResource(dataRes).forEach { (queryStr, results) ->
                println("Fit testing $queryStr")
                val (query, virtuals) = queryParser.parse(queryStr)

                val queryResolver = FallbackResolver.withVirtuals(virtuals, typeResolver)
                val fitter: QueryFitter = JavaQueryFitter(infoRepo, queryResolver)

                val fitting = functions.filter { function ->
                    fitter.fitsQuery(query, function) != null
                }.map {
                    it.info
                }.toSet()

                assertEquals(results, fitting)
            }
        }

    }

    @Test
    fun `java simple tests`() {
        verify(
                dataRes = "/test/verification/java-simple.txt",
                codeBase = "/test/codebase/java/"
        )
    }

}